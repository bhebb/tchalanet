import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { BadgeStatus, TchErrorPanel, TchStatusBadge } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';

import {
  PlanSummaryView,
  SubscriptionView,
} from '../../data-access/platform-tenant-contracts';
import { TenantPlanPickerComponent } from '../tenant-plan-picker/tenant-plan-picker.component';

@Component({
  selector: 'tch-tenant-subscription-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    DatePipe,
    MatButtonModule,
    AdminSectionCardComponent,
    TchStatusBadge,
    TchErrorPanel,
    TenantPlanPickerComponent,
  ],
  templateUrl: './tenant-subscription-panel.component.html',
  styleUrls: ['./tenant-subscription-panel.component.scss'],
})
export class TenantSubscriptionPanelComponent {
  readonly subscription = input<SubscriptionView | null>(null);
  readonly plans = input<readonly PlanSummaryView[]>([]);
  readonly showPicker = input(false);
  readonly selectedPlanCode = input<string | null>(null);
  readonly applying = input(false);
  readonly applyError = input<string | null>(null);

  readonly openPicker = output<void>();
  readonly cancelPicker = output<void>();
  readonly selectPlan = output<string>();
  readonly applyPlan = output<void>();

  statusBadge(status: string | null | undefined): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready',
      TRIAL: 'pending',
      SUSPENDED: 'warning',
      CANCELED: 'blocked',
      EXPIRED: 'missing',
    };
    return (status ? map[status] : null) ?? 'missing';
  }
}
