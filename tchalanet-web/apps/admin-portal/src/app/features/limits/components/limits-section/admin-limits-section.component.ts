import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ResourceRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  LimitBlockAssignment,
  LimitBlockSpec,
  LimitGroup,
  LimitPolicyBlockComponent,
  LimitPolicyEditRequest,
  LpbLabels,
} from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';

import { AdminLimitsApi, CombinedLimitData } from '../../data-access/admin-limits-api.service';
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
    TranslatePipe,
    TchSectionError,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    AdminSectionCardComponent,
    LimitPolicyBlockComponent,
  ],
  template: `
    <tch-admin-section-card [title]="sectionTitle()" icon="shield">
      <tch-async-view
        [resource]="combinedResource"
        [error]="loadError()"
        [loadingLabel]="'admin.limits.common.loading' | translate"
        [retryLabel]="'common.retry' | translate"
        (retry)="combinedResource.reload()"
      >
        <ng-template tchAsyncReady>
          @if (combinedResource.value(); as data) {
            <tch-limit-policy-block
              [specs]="filterSpecs(data.specs)"
              [assignments]="asBlockAssignments(data.assignments)"
              [inheritedAssignments]="data.inheritedAssignments ? asBlockAssignments(data.inheritedAssignments) : null"
              [inheritedScopeLabel]="inheritedScopeLabel()"
              [defaultExpandedGroups]="defaultExpandedGroups()"
              [groupLabels]="lpbGroupLabels()"
              [labels]="lpbLabels()"
              (editRequested)="onEdit($event)"
              (deleteRequested)="onDelete($event)"
            />
          }
        </ng-template>
      </tch-async-view>
      @if (actionError()) {
        <tch-section-error
          [title]="'admin.limits.section.actionError' | translate"
          [message]="actionError()!"
          severity="error"
        />
      }
    </tch-admin-section-card>
  `,
})
export class AdminLimitsSectionComponent {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly targetType = input.required<TargetType>();
  readonly targetId = input<string | null>(null);
  readonly inheritedTargetType = input<TargetType | null>(null);
  readonly inheritedTargetId = input<string | null>(null);
  readonly inheritedScopeLabel = input<string>();
  readonly allowedRuleKeys = input<RuleKey[] | null>(null);
  readonly defaultExpandedGroups = input<LimitGroup[]>([]);
  readonly sectionTitle = input.required<string>();

  readonly actionError = signal<string | null>(null);

  readonly combinedResource: ResourceRef<CombinedLimitData | undefined> =
    this.api.combinedLimitsResource({
      targetType: this.targetType,
      targetId: this.targetId,
      inheritedTargetType: this.inheritedTargetType,
      inheritedTargetId: this.inheritedTargetId,
    });

  readonly loadError = resourceErrorVm(this.combinedResource, 'admin.limits.section');

  readonly lpbGroupLabels = computed<Record<LimitGroup, string>>(() => ({
    VENTE: this.translate.instant('admin.limits.blockComponent.group.vente'),
    RESTRICTIONS: this.translate.instant('admin.limits.blockComponent.group.restrictions'),
    EXPOSITION: this.translate.instant('admin.limits.blockComponent.group.exposition'),
  }));

  readonly lpbLabels = computed<LpbLabels>(() => ({
    unconfigured: this.translate.instant('admin.limits.blockComponent.unconfigured'),
    configured: this.translate.instant('admin.limits.blockComponent.configured'),
    inheritedFrom: this.translate.instant('admin.limits.blockComponent.inheritedFrom'),
    editAria: this.translate.instant('admin.limits.blockComponent.editAria'),
    deleteAria: this.translate.instant('admin.limits.blockComponent.deleteAria'),
  }));

  asBlockAssignments(items: LimitAssignmentItem[]): LimitBlockAssignment[] {
    return items as unknown as LimitBlockAssignment[];
  }

  filterSpecs(specs: LimitRuleSpec[]): LimitBlockSpec[] {
    const allowed = this.allowedRuleKeys();
    return (allowed ? specs.filter(s => allowed.includes(s.ruleKey)) : specs) as LimitBlockSpec[];
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
        if (result) this.combinedResource.reload();
      });
  }

  onDelete(assignmentId: string): void {
    this.actionError.set(null);
    this.api.deleteAssignment(assignmentId, { suppressShellFeedback: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.combinedResource.reload(),
        error: () => this.actionError.set(this.translate.instant('admin.limits.section.actionError')),
      });
  }
}
