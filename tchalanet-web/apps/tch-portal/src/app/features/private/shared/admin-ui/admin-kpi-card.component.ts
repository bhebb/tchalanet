import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export interface AdminKpiCardStatus {
  readonly tone: 'neutral' | 'success' | 'warning' | 'danger';
  readonly label?: string;
  readonly icon?: string;
}

@Component({
  selector: 'tch-admin-kpi-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="kpi-card" [class.kpi-card--strong]="variant() === 'strong'">
      <div class="kpi-card__top">
        @if (icon()) {
          <div class="kpi-card__icon-wrap">
            <span class="kpi-card__icon material-symbols-outlined" aria-hidden="true">
              {{ icon() }}
            </span>
          </div>
        }
        @if (status()) {
          <span class="kpi-card__status" [attr.data-tone]="status()!.tone">
            @if (status()!.icon) {
              <span class="material-symbols-outlined">{{ status()!.icon }}</span>
            }
            @if (status()!.label) {
              {{ status()!.label }}
            }
          </span>
        }
      </div>

      <div class="kpi-card__body">
        <p class="kpi-card__label">{{ label() }}</p>
        <p class="kpi-card__value">
          @if (loading()) {
            <span class="kpi-card__skeleton" aria-busy="true">—</span>
          } @else {
            {{ value() ?? '—' }}
          }
        </p>
        @if (hint()) {
          <p class="kpi-card__hint">{{ hint() }}</p>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .kpi-card {
        background: var(--tch-color-surface-container-lowest, #ffffff);
        border: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        border-radius: var(--tch-radius-2xl, 1.5rem);
        padding: 1.5rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        transition: box-shadow 150ms ease;
      }

      .kpi-card:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      .kpi-card--strong {
        background: var(--tch-color-primary-container, #141545);
        border-color: transparent;
      }

      .kpi-card--strong .kpi-card__label {
        color: var(--tch-color-on-primary-container, #8384ba);
      }

      .kpi-card--strong .kpi-card__value {
        color: var(--tch-color-on-primary, #ffffff);
      }

      .kpi-card--strong .kpi-card__hint {
        color: var(--tch-color-on-primary-container, #8384ba);
      }

      .kpi-card__top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
      }

      .kpi-card__icon-wrap {
        width: 2.5rem;
        height: 2.5rem;
        border-radius: var(--tch-radius-lg, 0.5rem);
        background: var(--tch-color-surface-container, #edeef1);
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--tch-color-primary, #020135);
      }

      .kpi-card--strong .kpi-card__icon-wrap {
        background: rgba(255, 255, 255, 0.1);
        color: var(--tch-color-secondary-container, #fecb01);
      }

      .kpi-card__icon {
        font-size: 1.25rem;
        font-family: 'Material Symbols Outlined';
        font-variation-settings:
          'FILL' 0,
          'wght' 400,
          'GRAD' 0,
          'opsz' 24;
      }

      .kpi-card__status {
        font-size: 0.75rem;
        font-weight: 700;
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }

      .kpi-card__status[data-tone='success'] {
        color: var(--tch-status-ready, #10b981);
      }
      .kpi-card__status[data-tone='warning'] {
        color: var(--tch-status-warning, #f59e0b);
      }
      .kpi-card__status[data-tone='danger'] {
        color: var(--tch-status-blocked, #dc2626);
      }
      .kpi-card__status[data-tone='neutral'] {
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .kpi-card__status .material-symbols-outlined {
        font-size: 1rem;
        font-family: 'Material Symbols Outlined';
      }

      .kpi-card__label {
        margin: 0;
        font-size: 0.6875rem;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .kpi-card__value {
        margin: 0.25rem 0 0;
        font-size: 2rem;
        font-weight: 700;
        font-family: 'JetBrains Mono', monospace;
        color: var(--tch-color-on-surface, #1a1c1e);
        line-height: 1.1;
      }

      .kpi-card__hint {
        margin: 0.25rem 0 0;
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .kpi-card__skeleton {
        opacity: 0.4;
      }
    `,
  ],
})
export class AdminKpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input<string | number | null>(null);
  readonly hint = input<string | null>(null);
  readonly icon = input<string | null>(null);
  readonly status = input<AdminKpiCardStatus | null>(null);
  readonly loading = input(false);
  readonly variant = input<'default' | 'strong'>('default');
}
