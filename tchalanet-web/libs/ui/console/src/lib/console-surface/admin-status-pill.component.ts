import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';

export type AdminStatusTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

const TONE_TO_STATUS: Record<AdminStatusTone, BadgeStatus> = {
  success: 'ready',
  warning: 'warning',
  danger: 'blocked',
  neutral: 'missing',
  info: 'pending',
};

@Component({
  selector: 'tch-admin-status-pill',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchStatusBadge],
  template: `<tch-status-badge [status]="badgeStatus()" [label]="label()" />`,
})
export class AdminStatusPillComponent {
  readonly label = input.required<string>();
  readonly tone = input<AdminStatusTone>('neutral');

  readonly badgeStatus = computed<BadgeStatus>(() => TONE_TO_STATUS[this.tone()]);
}
