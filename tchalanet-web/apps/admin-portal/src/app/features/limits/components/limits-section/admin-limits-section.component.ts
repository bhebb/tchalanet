import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnChanges,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchSectionError } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  LimitBlockAssignment,
  LimitBlockSpec,
  LimitGroup,
  LimitPolicyBlockComponent,
  LimitPolicyEditRequest,
} from '@tch/ui/console';
import { resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type {
  LimitAssignmentItem,
  LimitRuleSpec,
  RuleKey,
  TargetType,
} from '../../data-access/admin-limits.models';
import { UpsertLimitDialogComponent } from '../upsert-limit-dialog/upsert-limit-dialog.component';

@Component({
  selector: 'tch-admin-limits-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TchLoading,
    TchSectionError,
    AdminSectionCardComponent,
    LimitPolicyBlockComponent,
  ],
  template: `
    <tch-admin-section-card [title]="sectionTitle()" icon="shield">
      @if (loading()) {
        <tch-loading label="Chargement des limites..." />
      } @else if (loadError()) {
        <tch-section-error
          title="Erreur de chargement"
          [message]="loadError()!"
        />
      } @else {
        <tch-limit-policy-block
          [specs]="filteredSpecs()"
          [assignments]="assignments()"
          [inheritedAssignments]="inheritedAssignments()"
          [inheritedScopeLabel]="inheritedScopeLabel()"
          [defaultExpandedGroups]="defaultExpandedGroups()"
          (editRequested)="onEdit($event)"
          (deleteRequested)="onDelete($event)"
        />
      }
      @if (actionError()) {
        <tch-section-error
          title="Erreur"
          [message]="actionError()!"
          severity="error"
        />
      }
    </tch-admin-section-card>
  `,
})
export class AdminLimitsSectionComponent implements OnChanges {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly targetType = input.required<TargetType>();
  readonly targetId = input<string | null>(null);
  readonly inheritedTargetType = input<TargetType | null>(null);
  readonly inheritedTargetId = input<string | null>(null);
  readonly inheritedScopeLabel = input('tenant');
  readonly allowedRuleKeys = input<RuleKey[] | null>(null);
  readonly defaultExpandedGroups = input<LimitGroup[]>([]);
  readonly sectionTitle = input('Limites');

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  private readonly allSpecs = signal<LimitRuleSpec[]>([]);
  readonly assignments = signal<LimitBlockAssignment[]>([]);
  readonly inheritedAssignments = signal<LimitBlockAssignment[] | null>(null);

  readonly filteredSpecs = computed<LimitBlockSpec[]>(() => {
    const allowed = this.allowedRuleKeys();
    const specs = this.allSpecs();
    return allowed ? specs.filter(s => allowed.includes(s.ruleKey)) : specs;
  });

  ngOnChanges(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.loadError.set(null);

    const target = this.targetType();
    const targetId = this.targetId() ?? undefined;
    const inheritedType = this.inheritedTargetType();
    const inheritedId = this.inheritedTargetId() ?? undefined;

    if (inheritedType) {
      forkJoin([
        this.api.listRules(),
        this.api.listAssignments(target, targetId),
        this.api.listAssignments(inheritedType, inheritedId),
      ])
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: ([specs, current, inherited]) => {
            this.allSpecs.set(specs);
            this.assignments.set(current.items as LimitBlockAssignment[]);
            this.inheritedAssignments.set(inherited.items as LimitBlockAssignment[]);
            this.loading.set(false);
          },
          error: (err: unknown) => {
            this.loadError.set(this.resolveError(err));
            this.loading.set(false);
          },
        });
    } else {
      forkJoin([this.api.listRules(), this.api.listAssignments(target, targetId)])
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: ([specs, current]) => {
            this.allSpecs.set(specs);
            this.assignments.set(current.items as LimitBlockAssignment[]);
            this.inheritedAssignments.set(null);
            this.loading.set(false);
          },
          error: (err: unknown) => {
            this.loadError.set(this.resolveError(err));
            this.loading.set(false);
          },
        });
    }
  }

  onEdit(req: LimitPolicyEditRequest): void {
    const dialogRef = this.dialog.open(UpsertLimitDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
    });
    const instance = dialogRef.componentInstance;
    instance.init(
      req.spec as LimitRuleSpec,
      this.targetType(),
      this.targetId(),
      req.assignment as LimitAssignmentItem | null,
    );
    dialogRef.afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result) this.load();
      });
  }

  onDelete(assignmentId: string): void {
    this.actionError.set(null);
    this.api.deleteAssignment(assignmentId, { suppressShellFeedback: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.load(),
        error: (err: unknown) => this.actionError.set(this.resolveError(err)),
      });
  }

  private resolveError(err: unknown): string {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.section', 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy).message;
  }
}
