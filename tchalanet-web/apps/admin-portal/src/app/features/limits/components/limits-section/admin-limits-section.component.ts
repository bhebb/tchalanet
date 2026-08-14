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
import { formatActiveLimitParams } from '../../data-access/admin-limits.models';
import { UpsertLimitDialogComponent } from '../upsert-limit-dialog/upsert-limit-dialog.component';

interface ActiveLimitRow {
  readonly item: LimitAssignmentItem;
  readonly inherited: boolean;
}

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
            @let rows = activeRows(data);
            @if (rows.length) {
              <section
                class="limits-section__active"
                [attr.aria-label]="'admin.limits.section.activeAria' | translate"
              >
                <header class="limits-section__active-head">
                  <div>
                    <h3>{{ 'admin.limits.section.activeTitle' | translate }}</h3>
                    <p>{{ 'admin.limits.section.activeDescription' | translate }}</p>
                  </div>
                  <span>{{ rows.length }}</span>
                </header>

                <div class="limits-section__active-list">
                  @for (row of rows; track activeRowTrack(row)) {
                    <article
                      class="limits-section__active-row"
                      [class.limits-section__active-row--inherited]="row.inherited"
                    >
                      <span class="material-symbols-outlined" aria-hidden="true">
                        {{ activeIcon(row.item.ruleKey) }}
                      </span>
                      <div class="limits-section__active-copy">
                        <strong>{{ activeLabel(row.item) }}</strong>
                      </div>
                      <small>{{ breachLabel(row.item.onBreach) }} · {{ activeSourceLabel(row) }}</small>
                    </article>
                  }
                </div>
              </section>
            }

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
  styles: [`
    @use 'breakpoints' as ui;

    .limits-section__active {
      display: grid;
      gap: 0.75rem;
      margin-block-end: 1rem;
      padding: 0.875rem;
      border: 1px solid var(--tch-color-outline-variant, rgba(23, 23, 31, 0.16));
      border-radius: 0.75rem;
      background: var(--tch-color-surface-container-lowest, #fff);
    }

    .limits-section__active-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
    }

    .limits-section__active-head h3,
    .limits-section__active-head p {
      margin: 0;
    }

    .limits-section__active-head h3 {
      font-size: 0.95rem;
      font-weight: 800;
      color: var(--tch-color-on-surface, #1b1b1e);
    }

    .limits-section__active-head p {
      margin-block-start: 0.125rem;
      color: var(--tch-color-on-surface-variant, #55545f);
      font-size: 0.85rem;
      line-height: 1.35;
    }

    .limits-section__active-head > span {
      display: inline-flex;
      min-width: 2rem;
      min-height: 2rem;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      background: var(--tch-color-primary-container, #d7e2ff);
      color: var(--tch-color-on-primary-container, #001b3f);
      font-weight: 800;
    }

    .limits-section__active-list {
      display: grid;
      gap: 0.5rem;
    }

    .limits-section__active-row {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr);
      gap: 0.625rem;
      align-items: center;
      padding: 0.75rem;
      border-radius: 0.625rem;
      background: var(--tch-color-surface-container-low, #f5f3f7);
    }

    .limits-section__active-row > .material-symbols-outlined {
      color: var(--tch-color-primary, #00458f);
      font-size: 1.35rem;
    }

    .limits-section__active-copy {
      display: grid;
      min-width: 0;
      gap: 0.125rem;
    }

    .limits-section__active-copy strong,
    .limits-section__active-copy span {
      overflow-wrap: anywhere;
    }

    .limits-section__active-copy strong {
      font-weight: 800;
      color: var(--tch-color-on-surface, #1b1b1e);
    }

    .limits-section__active-copy span,
    .limits-section__active-row small {
      color: var(--tch-color-on-surface-variant, #55545f);
    }

    .limits-section__active-row small {
      grid-column: 2;
      font-weight: 700;
    }

    .limits-section__active-row--inherited {
      border: 1px dashed var(--tch-color-outline-variant, rgba(23, 23, 31, 0.16));
      background: transparent;
    }

    @include ui.up(medium) {
      .limits-section__active-row {
        grid-template-columns: auto minmax(0, 1fr) auto;
      }

      .limits-section__active-row small {
        grid-column: auto;
      }
    }
  `],
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
  readonly effectiveAt = input<string | null>(null);
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

  activeRows(data: CombinedLimitData): ActiveLimitRow[] {
    const current = data.assignments
      .filter(item => item.enabled && this.isEffective(item))
      .map(item => ({ item, inherited: false }));
    const inherited = (data.inheritedAssignments ?? [])
      .filter(item => item.enabled && this.isEffective(item))
      .map(item => ({ item, inherited: true }));
    return [...current, ...inherited].sort((a, b) => {
      if (a.item.ruleKey === 'BLOCK_SELECTION_PER_DRAW') return -1;
      if (b.item.ruleKey === 'BLOCK_SELECTION_PER_DRAW') return 1;
      return a.item.ruleKey.localeCompare(b.item.ruleKey);
    });
  }

  activeRowTrack(row: ActiveLimitRow): string {
    return `${row.inherited ? 'inherited' : 'current'}-${row.item.id.value}`;
  }

  activeIcon(ruleKey: RuleKey): string {
    if (ruleKey === 'BLOCK_SELECTION_PER_DRAW' || ruleKey === 'BLOCK_BET_TYPE') return 'block';
    if (ruleKey.includes('STAKE') || ruleKey.includes('EXPOSURE')) return 'payments';
    if (ruleKey.includes('TICKET') || ruleKey.includes('LINES')) return 'confirmation_number';
    return 'shield';
  }

  activeParamsLabel(item: LimitAssignmentItem): string {
    const value = formatActiveLimitParams({
      assignmentId: item.id.value,
      ruleKey: item.ruleKey,
      group: 'ADVANCED',
      targetType: this.targetType(),
      targetId: this.targetId() ?? '',
      targetLabel: null,
      targetCode: null,
      enabled: item.enabled,
      onBreach: item.onBreach,
      params: item.params,
      startsAt: item.startsAt,
      endsAt: item.endsAt,
      actions: [],
    });
    return value === '—'
      ? this.translate.instant('admin.limits.section.noParams')
      : value;
  }

  activeLabel(item: LimitAssignmentItem): string {
    const params = this.activeParamsLabel(item);
    if (item.ruleKey === 'BLOCK_SELECTION_PER_DRAW') {
      return this.translate.instant('admin.limits.section.blockedNumbers', { numbers: params });
    }
    return `${this.translate.instant(this.ruleLabelKey(item.ruleKey))}: ${params}`;
  }

  activeSourceLabel(row: ActiveLimitRow): string {
    if (!row.inherited) return this.translate.instant('admin.limits.section.currentScope');
    const scope = this.inheritedScopeLabel() ?? this.translate.instant('admin.limits.section.scope.tenant');
    return this.translate.instant('admin.limits.section.inheritedScope', { scope });
  }

  breachLabel(outcome: LimitAssignmentItem['onBreach']): string {
    if (outcome === 'BLOCK') return this.translate.instant('admin.limits.dialog.block');
    if (outcome === 'WARN') return this.translate.instant('admin.limits.dialog.warn');
    if (outcome === 'REQUIRE_APPROVAL') {
      return this.translate.instant('admin.limits.table.approvalRequired');
    }
    if (outcome === 'ALLOW') return this.translate.instant('admin.limits.table.allow');
    return this.translate.instant('admin.limits.table.undefined');
  }

  ruleLabelKey(ruleKey: RuleKey): string {
    return `admin.limits.rule.${ruleKey}`;
  }

  private isEffective(item: LimitAssignmentItem): boolean {
    const effectiveAt = this.effectiveAt();
    if (!effectiveAt) return true;
    const at = Date.parse(effectiveAt);
    if (Number.isNaN(at)) return true;
    if (item.startsAt) {
      const start = Date.parse(item.startsAt);
      if (!Number.isNaN(start) && at < start) return false;
    }
    if (item.endsAt) {
      const end = Date.parse(item.endsAt);
      if (!Number.isNaN(end) && at > end) return false;
    }
    return true;
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
