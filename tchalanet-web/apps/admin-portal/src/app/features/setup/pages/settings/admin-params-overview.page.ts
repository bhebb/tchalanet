import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { TenantParametersApiService } from '../../data-access/tenant-parameters-api.service';

@Component({
  selector: 'tch-admin-params-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
  ],
  templateUrl: './admin-params-overview.page.html',
  styleUrls: ['./admin-params-overview.page.scss'],
})
export class AdminParamsOverviewPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly route = inject(ActivatedRoute);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/business-profile';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.businessProfile.title';
  readonly linkQueryParams = this.fromSetup ? { from: 'setup' } : undefined;

  readonly config = this.api.tenantConfigResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;

  readonly receiptSummary = computed(() => {
    const r = this.config.value()?.document?.receipt;
    if (!r) return null;
    return {
      enabled: r.enabled ?? true,
      paperSize: this.paperSizeLabel(r.defaultPaperSize ?? null),
      qrCode: r.showQrCode ?? true,
    };
  });

  readonly deliverySummary = computed(() => {
    const delivery = this.config.value()?.communication?.buyerTicketDelivery;
    if (!delivery) return null;
    return {
      sms: delivery.sms?.enabled ?? false,
      whatsapp: delivery.whatsapp?.enabled ?? false,
      email: delivery.email?.enabled ?? false,
    };
  });

  readonly calendarSummary = computed(() => {
    const cal = this.config.value()?.rules?.businessCalendar;
    if (!cal) return null;
    return {
      defaultOpen: cal.defaultOpen ?? true,
      closedWeekdayCount: (cal.closedWeekdays ?? []).length,
      holidayCount: (cal.holidays ?? []).length,
    };
  });

  paperSizeLabel(size: string | null): string {
    const map: Record<string, string> = {
      RECEIPT_58MM: '58 mm',
      RECEIPT_80MM: '80 mm',
      A4: 'A4',
    };
    return size ? (map[size] ?? size) : '80 mm';
  }
}
