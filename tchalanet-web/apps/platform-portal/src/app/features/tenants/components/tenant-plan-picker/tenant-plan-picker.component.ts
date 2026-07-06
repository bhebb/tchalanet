import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { PlanSummaryView } from '../../data-access/platform-tenant-contracts';

@Component({
  selector: 'tch-tenant-plan-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './tenant-plan-picker.component.html',
  styleUrls: ['./tenant-plan-picker.component.scss'],
})
export class TenantPlanPickerComponent {
  readonly plans = input.required<readonly PlanSummaryView[]>();
  readonly selected = input<string | null>(null);
  readonly loading = input(false);
  readonly selectedChange = output<string>();

  select(code: string): void {
    this.selectedChange.emit(code);
  }

  priceLabel(plan: PlanSummaryView): string | null {
    if (plan.priceAmount == null) return null;
    const amount = plan.priceAmount === 0 ? '0' : plan.priceAmount.toString();
    const period = plan.billingPeriod ? `/${plan.billingPeriod.toLowerCase()}` : '';
    return `${amount} ${plan.currency ?? ''}${period}`.trim();
  }
}
