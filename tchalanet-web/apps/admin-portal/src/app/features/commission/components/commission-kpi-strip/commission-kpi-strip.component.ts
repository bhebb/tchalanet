import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminMetricCardComponent } from '@tch/ui/console';

import { CommissionOverviewView } from '../../data-access/admin-commission-api.service';

@Component({
  selector: 'tch-commission-kpi-strip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminMetricCardComponent, DecimalPipe, TranslatePipe],
  templateUrl: './commission-kpi-strip.component.html',
  styleUrls: ['./commission-kpi-strip.component.scss'],
})
export class CommissionKpiStripComponent {
  readonly overview = input.required<CommissionOverviewView>();

  rate(value: number | null): string {
    return value == null ? '—' : `${value}%`;
  }
}
