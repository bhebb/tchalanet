import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type BadgeStatus = 'ready' | 'warning' | 'blocked' | 'missing' | 'pending';

@Component({
  selector: 'tch-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [attr.data-status]="status()">{{ label() }}</span>`,
  styles: [
    `
      .badge {
        display: inline-flex;
        align-items: center;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.625rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        font-weight: 700;
        background: var(--tch-color-surface-container-low, #f3f3f6);
        color: var(--tch-color-on-surface-variant, #464652);
      }
      .badge[data-status='ready'] {
        color: var(--tch-color-status-ready, #10b981);
      }
      .badge[data-status='warning'],
      .badge[data-status='pending'] {
        color: var(--tch-color-status-warning, #f59e0b);
      }
      .badge[data-status='blocked'] {
        color: var(--tch-color-status-blocked, #dc2626);
      }
      .badge[data-status='missing'] {
        color: var(--tch-color-status-missing, #64748b);
      }
    `,
  ],
})
export class TchStatusBadge {
  readonly status = input<BadgeStatus>('missing');
  readonly label = input('');
}
