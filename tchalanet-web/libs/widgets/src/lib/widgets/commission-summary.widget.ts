import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp } from '@tch/page-model';

function numVal(v: unknown): number | null {
  if (typeof v === 'number') return v;
  if (typeof v === 'string') {
    const n = parseFloat(v);
    return isNaN(n) ? null : n;
  }
  return null;
}

@Component({
  selector: 'tch-commission-summary-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  template: `
    <div class="commission-summary">
      <div class="commission-summary__header">
        <h2 class="commission-summary__title">{{ titleKey() | tchLabel }}</h2>
        @if (manageLink()) {
          <a class="commission-summary__manage-link" [routerLink]="manageLink()">
            {{ 'common.manage' | tchLabel }}
          </a>
        }
      </div>

      <div class="commission-summary__body">
        <div class="cs-default-rate">
          <span class="cs-default-rate__label">{{ 'dashboard.tenant_admin.commission.default_rate' | tchLabel }}</span>
          @if (tenantDefaultRate() !== null) {
            <span class="cs-default-rate__value">{{ tenantDefaultRate() }}%</span>
          } @else {
            <span class="cs-default-rate__value cs-default-rate__value--unset">
              {{ 'dashboard.tenant_admin.commission.default_not_set' | tchLabel }}
            </span>
          }
        </div>

        <div class="cs-stats">
          <div class="cs-stat">
            <span class="cs-stat__value">{{ totalSellerTerminals() }}</span>
            <span class="cs-stat__label">{{ 'dashboard.tenant_admin.commission.seller_terminals' | tchLabel }}</span>
          </div>
          <div class="cs-stat">
            <span class="cs-stat__value">{{ countAtDefaultRate() }}</span>
            <span class="cs-stat__label">{{ 'dashboard.tenant_admin.commission.at_default' | tchLabel }}</span>
          </div>
          <div class="cs-stat">
            <span class="cs-stat__value">{{ countWithCustomRate() }}</span>
            <span class="cs-stat__label">{{ 'dashboard.tenant_admin.commission.custom' | tchLabel }}</span>
          </div>
          @if (rateRange()) {
            <div class="cs-stat">
              <span class="cs-stat__value">{{ rateRange() }}</span>
              <span class="cs-stat__label">{{ 'dashboard.tenant_admin.commission.rate_range' | tchLabel }}</span>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .commission-summary {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .commission-summary__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.5rem;
      }

      .commission-summary__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .commission-summary__manage-link {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 600;
        color: var(--tch-color-primary, #1a6ef7);
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }

      .commission-summary__body {
        display: flex;
        flex-direction: column;
        gap: 0.875rem;
      }

      .cs-default-rate {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.875rem 1rem;
        border-radius: var(--tch-radius-md, 12px);
        background: var(--tch-color-primary-container, #d8e2ff);
        border: 1px solid var(--tch-color-outline-variant, #e0e0e8);
      }

      .cs-default-rate__label {
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-primary-container, #001849);
        font-weight: 500;
      }

      .cs-default-rate__value {
        font-size: var(--tch-font-size-title-lg, 1.375rem);
        font-weight: 700;
        color: var(--tch-color-on-primary-container, #001849);
      }

      .cs-default-rate__value--unset {
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        font-weight: 400;
        color: var(--tch-color-outline, #757780);
        font-style: italic;
      }

      .cs-stats {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(7rem, 1fr));
        gap: 0.5rem;
      }

      .cs-stat {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
        padding: 0.75rem 0.5rem;
        border-radius: var(--tch-radius-sm, 8px);
        background: var(--tch-color-surface-container-low, #f3f3f7);
        border: 1px solid var(--tch-color-outline-variant, #e0e0e8);
        text-align: center;
      }

      .cs-stat__value {
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 700;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .cs-stat__label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, #44464f);
        font-weight: 500;
      }
    `,
  ],
})
export class CommissionSummaryWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly manageLink = computed(() => stringProp(this.config(), 'manageLink') ?? null);

  readonly tenantDefaultRate = computed<number | null>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return null;
    const val = numVal(dyn['tenantDefaultRate']);
    return val ?? null;
  });

  readonly totalSellerTerminals = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['totalSellerTerminals']) ?? 0;
  });

  readonly countAtDefaultRate = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['countAtDefaultRate']) ?? 0;
  });

  readonly countWithCustomRate = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['countWithCustomRate']) ?? 0;
  });

  readonly rateRange = computed<string | null>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return null;
    const min = numVal(dyn['minRate']);
    const max = numVal(dyn['maxRate']);
    if (min === null || max === null) return null;
    return `${min}% – ${max}%`;
  });
}
