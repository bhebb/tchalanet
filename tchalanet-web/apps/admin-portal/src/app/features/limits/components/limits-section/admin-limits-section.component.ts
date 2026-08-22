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
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';

import { AdminLimitsApi, CombinedLimitData } from '../../data-access/admin-limits-api.service';
import type {
  LimitAssignmentItem,
  LimitRuleSpec,
  RuleKey,
  TargetType,
} from '../../data-access/admin-limits.models';
import { formatActiveLimitParams } from '../../data-access/admin-limits.models';
import {
  BlockNumberQuickDialogComponent,
  type BlockNumberQuickDialogData,
} from '../block-number-quick-dialog/block-number-quick-dialog.component';
import { UpsertLimitDialogComponent } from '../upsert-limit-dialog/upsert-limit-dialog.component';

// LimitGroup kept as input type for backwards compat with callers that still pass it
type LimitGroup = string;

@Component({
  selector: 'tch-admin-limits-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    MatButtonModule,
    TchSectionError,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    AdminSectionCardComponent,
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
            @let local = localRows(data);
            @let inherited = inheritedRows(data);

            @if (local.length) {
              <section class="ls__group">
                <h4 class="ls__group-title">
                  {{ localGroupTitleKey() | translate }}
                </h4>
                <ul class="ls__rows" role="list">
                  @for (item of local; track item.id.value) {
                    <li class="ls__row" [routerLink]="['/app/admin/limits', item.id.value]">
                      <span class="material-symbols-outlined ls__icon" aria-hidden="true">
                        {{ ruleIcon(item.ruleKey) }}
                      </span>
                      <div class="ls__row-copy">
                        <strong>{{ ruleLabel(item.ruleKey) | translate }}</strong>
                        <span>{{ paramsLabel(item) }}</span>
                      </div>
                    </li>
                  }
                </ul>
              </section>
            }

            @if (inherited.length) {
              <section class="ls__group">
                <h4 class="ls__group-title">
                  {{ 'admin.limits.section.inherited.title' | translate }}
                </h4>
                <ul class="ls__rows" role="list">
                  @for (item of inherited; track item.id.value) {
                    <li class="ls__row ls__row--inherited">
                      <span class="material-symbols-outlined ls__icon" aria-hidden="true">
                        {{ ruleIcon(item.ruleKey) }}
                      </span>
                      <div class="ls__row-copy">
                        <strong>{{ ruleLabel(item.ruleKey) | translate }}</strong>
                        <span>{{ paramsLabel(item) }}</span>
                      </div>
                      <small class="ls__provenance">
                        {{ 'admin.limits.section.inheritedScope' | translate : { scope: inheritedScopeLabel() ?? tenantLabel() } }}
                      </small>
                    </li>
                  }
                </ul>
              </section>
            }

            @if (!local.length && !inherited.length) {
              <p class="ls__empty">
                {{ 'admin.limits.section.empty' | translate }}
              </p>
            }

            <div class="ls__actions">
              @if (canBlockNumbers() && !hasBlockRule(local)) {
                <button mat-stroked-button type="button" (click)="openBlockNumber()">
                  <span class="material-symbols-outlined">block</span>
                  {{ 'admin.limits.section.blockNumber' | translate }}
                </button>
              }
              <button mat-flat-button color="primary" type="button" (click)="addLimit(data)">
                <span class="material-symbols-outlined">add</span>
                {{ 'admin.limits.section.add' | translate }}
              </button>
            </div>
            <a class="ls__manage-link" routerLink="/app/admin/limits">
              {{ 'admin.limits.section.manage' | translate }}
              <span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
            </a>
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

    .ls__group {
      margin-bottom: 1rem;
    }

    .ls__group-title {
      margin: 0 0 0.5rem;
      color: var(--tch-color-on-surface-variant);
      font-size: var(--tch-font-size-label-sm);
      font-weight: var(--tch-weight-semibold, 600);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .ls__rows {
      display: grid;
      gap: 0.5rem;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .ls__row {
      display: grid;
      grid-template-columns: auto minmax(0, 1fr);
      gap: 0.5rem 0.625rem;
      align-items: center;
      padding: 0.75rem;
      border-radius: var(--tch-radius-md);
      background: var(--tch-color-surface-container-low);
      cursor: pointer;
      text-decoration: none;
      color: inherit;
      transition: background 0.12s;

      &:hover {
        background: var(--tch-color-surface-container);
      }
    }

    .ls__row--inherited {
      background: transparent;
      border: 1px dashed var(--tch-color-outline-variant);
    }

    .ls__icon {
      color: var(--tch-color-primary);
      font-size: 1.25rem;
    }

    .ls__row--inherited .ls__icon {
      color: var(--tch-color-on-surface-variant);
    }

    .ls__row-copy {
      display: grid;
      gap: 0.125rem;
      min-width: 0;

      strong {
        color: var(--tch-color-on-surface);
        font-size: var(--tch-font-size-body-md);
        overflow-wrap: anywhere;
      }

      span {
        color: var(--tch-color-on-surface-variant);
        font-size: var(--tch-font-size-body-sm);
        overflow-wrap: anywhere;
      }
    }

    .ls__provenance {
      grid-column: 2;
      color: var(--tch-color-on-surface-variant);
      font-size: var(--tch-font-size-label-sm);
      font-weight: var(--tch-weight-semibold, 600);
    }

    .ls__empty {
      margin: 0 0 1rem;
      color: var(--tch-color-on-surface-variant);
      font-size: var(--tch-font-size-body-sm);
    }

    .ls__actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.625rem;
      margin-top: 0.25rem;
    }

    .ls__manage-link {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      margin-top: 0.5rem;
      font-size: var(--tch-font-size-body-sm);
      color: var(--tch-color-primary);
      text-decoration: none;

      .material-symbols-outlined {
        font-size: 1rem;
      }

      &:hover {
        text-decoration: underline;
      }
    }

    @include ui.up(medium) {
      .ls__row {
        grid-template-columns: auto minmax(0, 1fr) auto;
        align-items: center;
      }

      .ls__provenance {
        grid-column: auto;
        white-space: nowrap;
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
  readonly targetLabel = input<string | null>(null);
  readonly inheritedTargetType = input<TargetType | null>(null);
  readonly inheritedTargetId = input<string | null>(null);
  readonly inheritedScopeLabel = input<string | undefined>(undefined);
  readonly allowedRuleKeys = input<RuleKey[] | null>(null);
  readonly effectiveAt = input<string | null>(null);
  // Kept for backwards compat with callers — no longer drives the template
  readonly defaultExpandedGroups = input<LimitGroup[]>([]);
  readonly sectionTitle = input.required<string>();

  readonly actionError = signal<string | null>(null);

  readonly canBlockNumbers = computed(() => {
    if (this.targetType() === 'AGENT') return false;
    const allowed = this.allowedRuleKeys();
    return !allowed || allowed.includes('BLOCK_SELECTION_PER_DRAW');
  });

  readonly localGroupTitleKey = computed(() => {
    const tt = this.targetType();
    if (tt === 'TENANT') return 'admin.limits.section.localTitle.TENANT';
    if (tt === 'DRAW_CHANNEL') return 'admin.limits.section.localTitle.DRAW_CHANNEL';
    if (tt === 'SELLER_TERMINAL') return 'admin.limits.section.localTitle.SELLER_TERMINAL';
    return 'admin.limits.section.local.title';
  });

  readonly combinedResource: ResourceRef<CombinedLimitData | undefined> =
    this.api.combinedLimitsResource({
      targetType: this.targetType,
      targetId: this.targetId,
      inheritedTargetType: this.inheritedTargetType,
      inheritedTargetId: this.inheritedTargetId,
    });

  readonly loadError = resourceErrorVm(this.combinedResource, 'admin.limits.section');

  tenantLabel(): string {
    return this.translate.instant('admin.limits.section.scope.tenant');
  }

  localRows(data: CombinedLimitData): LimitAssignmentItem[] {
    const allowed = this.allowedRuleKeys();
    return data.assignments
      .filter(item => item.enabled && this.isEffective(item))
      .filter(item => !allowed || allowed.includes(item.ruleKey));
  }

  inheritedRows(data: CombinedLimitData): LimitAssignmentItem[] {
    const allowed = this.allowedRuleKeys();
    return (data.inheritedAssignments ?? [])
      .filter(item => item.enabled && this.isEffective(item))
      .filter(item => !allowed || allowed.includes(item.ruleKey));
  }

  ruleLabel(ruleKey: RuleKey): string {
    return `admin.limits.rule.${ruleKey}.label`;
  }

  ruleIcon(ruleKey: RuleKey): string {
    if (ruleKey === 'BLOCK_SELECTION_PER_DRAW') return 'block';
    if (ruleKey.includes('STAKE') || ruleKey.includes('EXPOSURE')) return 'payments';
    return 'shield';
  }

  hasBlockRule(rows: LimitAssignmentItem[]): boolean {
    return rows.some(r => r.ruleKey === 'BLOCK_SELECTION_PER_DRAW');
  }

  paramsLabel(item: LimitAssignmentItem): string {
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
    if (value !== '—') return value;
    return this.translate.instant('admin.limits.section.noParams');
  }

  openBlockNumber(): void {
    const tt = this.targetType() as 'TENANT' | 'DRAW_CHANNEL' | 'SELLER_TERMINAL';
    const dialogData: BlockNumberQuickDialogData =
      tt === 'TENANT'
        ? { lockedTargetType: 'TENANT' }
        : {
            lockedTargetType: tt,
            lockedTargetId: this.targetId() ?? undefined,
            lockedTargetLabel: this.targetLabel() ?? undefined,
          };

    const ref = this.dialog.open(BlockNumberQuickDialogComponent, {
      width: '560px',
      maxWidth: '100vw',
      data: dialogData,
    });
    ref.afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result) this.combinedResource.reload();
      });
  }

  addLimit(data: CombinedLimitData): void {
    const allowed = this.allowedRuleKeys();
    const specs: LimitRuleSpec[] = (allowed
      ? data.specs.filter(s => allowed.includes(s.ruleKey))
      : data.specs) as LimitRuleSpec[];

    const ref = this.dialog.open(UpsertLimitDialogComponent, {
      width: '560px',
      maxWidth: '100vw',
    });
    ref.componentInstance.initAdd(specs, this.targetType(), this.targetId());
    ref.afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result) this.combinedResource.reload();
      });
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
}
