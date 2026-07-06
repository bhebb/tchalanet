import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-tenant-commercial-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent],
  templateUrl: './tenant-commercial-summary.component.html',
  styleUrls: ['./tenant-commercial-summary.component.scss'],
})
export class TenantCommercialSummaryComponent {
  readonly defaultCommissionRate = input<number | null>(null);
}
