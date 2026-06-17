import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type AdminStatusCode = 'ACTIVE' | 'PENDING' | 'BLOCKED' | 'DISABLED' | 'INACTIVE' | string;

type StatusStyle = { bg: string; fg: string };

const STATUS_STYLES: Record<string, StatusStyle> = {
  ACTIVE:   { bg: 'var(--tch-color-status-ready-container, #dcfce7)',   fg: 'var(--tch-color-status-ready, #15803d)' },
  PENDING:  { bg: 'var(--tch-color-status-warning-container, #fef9c3)', fg: 'var(--tch-color-status-warning, #a16207)' },
  BLOCKED:  { bg: 'var(--tch-color-error-container, #ffdad6)',          fg: 'var(--tch-color-on-error-container, #93000a)' },
  DISABLED: { bg: 'var(--tch-color-surface-container, #ededf4)',        fg: 'var(--tch-color-on-surface-variant, #46464f)' },
  INACTIVE: { bg: 'var(--tch-color-surface-container, #ededf4)',        fg: 'var(--tch-color-on-surface-variant, #46464f)' },
};

const FALLBACK: StatusStyle = {
  bg: 'var(--tch-color-surface-container)',
  fg: 'var(--tch-color-on-surface-variant)',
};

@Component({
  selector: 'tch-admin-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="status-badge"
      [style.background]="style().bg"
      [style.color]="style().fg"
    >
      {{ label() }}
    </span>
  `,
  styles: [
    `
      .status-badge {
        display: inline-flex;
        align-items: center;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.75rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        line-height: 1.2;
        white-space: nowrap;
      }
    `,
  ],
})
export class AdminStatusBadge {
  readonly code = input<AdminStatusCode>('');
  readonly label = input('');

  readonly style = computed<StatusStyle>(() => STATUS_STYLES[this.code()] ?? FALLBACK);
}
