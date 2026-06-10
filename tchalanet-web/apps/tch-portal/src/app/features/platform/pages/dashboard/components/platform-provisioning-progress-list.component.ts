import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PlatformProvisioningProgressItem } from '../platform-dashboard.model';

const STATUS_COLORS: Record<PlatformProvisioningProgressItem['status'], string> = {
  HEALTHY: 'var(--tch-status-ready, #10b981)',
  PROVISIONING: 'var(--tch-color-secondary-container, #fecb01)',
  PENDING: 'var(--tch-status-warning, #f59e0b)',
  BLOCKED: 'var(--tch-status-blocked, #dc2626)',
  FAILED: 'var(--tch-status-blocked, #dc2626)',
};

@Component({
  selector: 'tch-platform-provisioning-progress-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (items().length) {
      <ul class="progress-list">
        @for (item of items(); track item.tenantCode) {
          <li class="progress-item">
            <div class="progress-item__header">
              <div>
                <p class="progress-item__name">{{ item.tenantName }}</p>
                <p class="progress-item__code">{{ item.tenantCode }}</p>
              </div>
              <span class="progress-item__pct">{{ item.progressPercent }}%</span>
            </div>
            <div class="progress-item__bar-track">
              <div
                class="progress-item__bar-fill"
                [style.width.%]="item.progressPercent"
                [style.background]="statusColor(item.status)"
                role="progressbar"
                [attr.aria-valuenow]="item.progressPercent"
                aria-valuemin="0"
                aria-valuemax="100"
              ></div>
            </div>
          </li>
        }
      </ul>
    } @else {
      <p class="progress-list--empty">No provisioning in progress.</p>
    }
  `,
  styles: [
    `
      .progress-list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .progress-item__header {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        margin-bottom: 0.5rem;
      }

      .progress-item__name {
        margin: 0;
        font-weight: 600;
        font-size: 0.9375rem;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .progress-item__code {
        margin: 0;
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-family: 'JetBrains Mono', monospace;
      }

      .progress-item__pct {
        font-size: 0.9375rem;
        font-weight: 700;
        font-family: 'JetBrains Mono', monospace;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .progress-item__bar-track {
        height: 0.625rem;
        background: var(--tch-color-surface-container, #edeef1);
        border-radius: 9999px;
        overflow: hidden;
      }

      .progress-item__bar-fill {
        height: 100%;
        border-radius: 9999px;
        transition: width 800ms ease;
      }

      .progress-list--empty {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }
    `,
  ],
})
export class PlatformProvisioningProgressListComponent {
  readonly items = input.required<readonly PlatformProvisioningProgressItem[]>();

  statusColor(status: PlatformProvisioningProgressItem['status']): string {
    return STATUS_COLORS[status];
  }
}
