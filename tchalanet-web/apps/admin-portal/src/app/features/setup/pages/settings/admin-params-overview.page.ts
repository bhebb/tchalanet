import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { ConsoleFactsComponent, type ConsoleFact } from '@tch/web/console';
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
    ConsoleFactsComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
  ],
  templateUrl: './admin-params-overview.page.html',
  styleUrls: ['./admin-params-overview.page.scss'],
})
export class AdminParamsOverviewPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/business-profile';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.businessProfile.title';
  // Never forward from=setup to section sub-pages — back from delivery/receipt/calendar
  // should always return here, not bypass this overview and jump straight to setup.
  readonly linkQueryParams = undefined;

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

  readonly deliveryChannels = computed(() => {
    const delivery = this.config.value()?.communication?.buyerTicketDelivery;
    if (!delivery) return null;
    return {
      sms: delivery.sms?.enabled ?? false,
      whatsapp: delivery.whatsapp?.enabled ?? false,
      email: delivery.email?.enabled ?? false,
    };
  });

  readonly calendarFacts = computed<readonly ConsoleFact[]>(() => {
    const cal = this.config.value()?.rules?.businessCalendar;
    if (!cal) return [];
    const facts: ConsoleFact[] = [
      { label: this.t('admin.settings.overview.calendar.defaultOpen'), value: this.t((cal.defaultOpen ?? true) ? 'common.yes' : 'common.no') },
    ];
    const closedCount = (cal.closedWeekdays ?? []).length;
    if (closedCount > 0) {
      facts.push({ label: this.t('admin.settings.config.calendar.closedWeekdays'), value: String(closedCount) });
    }
    const holidayCount = (cal.holidays ?? []).length;
    if (holidayCount > 0) {
      facts.push({ label: this.t('admin.settings.config.calendar.recurringHolidays'), value: String(holidayCount) });
    }
    return facts;
  });

  private paperSizeLabel(size: string | null): string {
    const map: Record<string, string> = { RECEIPT_58MM: '58 mm', RECEIPT_80MM: '80 mm', A4: 'A4' };
    return size ? (map[size] ?? size) : '80 mm';
  }

  private t(key: string, params?: Record<string, string | number>): string {
    return this.translate.instant(key, params);
  }
}
