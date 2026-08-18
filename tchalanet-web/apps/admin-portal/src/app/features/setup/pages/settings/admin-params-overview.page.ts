import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { AdminPageShellComponent } from '@tch/ui/console';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TenantParametersApiService } from '../../data-access/tenant-parameters-api.service';
import {
  SettingsSummaryCardComponent,
  type SettingsSummaryFact,
  type SettingsSummarySectionVm,
} from './settings-summary-card.component';

@Component({
  selector: 'tch-admin-params-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    AdminPageShellComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    SettingsSummaryCardComponent,
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
  readonly linkQueryParams = undefined;

  readonly config = this.api.tenantConfigResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;

  readonly sections = computed<readonly SettingsSummarySectionVm[]>(() => {
    const cfg = this.config.value() ?? undefined;
    return [
      this.receiptSection(cfg),
      this.deliverySection(cfg),
      this.calendarSection(cfg),
    ];
  });

  private receiptSection(cfg: ReturnType<typeof this.config.value>): SettingsSummarySectionVm {
    const r = cfg?.document?.receipt;
    const active = this.t('admin.settings.overview.delivery.active');
    const inactive = this.t('admin.settings.overview.delivery.inactive');
    return {
      id: 'receipt',
      icon: 'receipt_long',
      title: this.t('admin.settings.overview.receipt.title'),
      route: '/app/admin/company/settings/receipt',
      queryParams: this.linkQueryParams,
      facts: r ? [
        this.badge(this.t('admin.settings.overview.receipt.enabled'), r.enabled ?? true, active, inactive),
        this.text(this.t('admin.settings.overview.receipt.paperSize'), this.paperSizeLabel(r.defaultPaperSize ?? null)),
        this.badge(this.t('admin.settings.overview.receipt.qrCode'), r.showQrCode ?? true, active, inactive),
      ] : null,
      emptyMessage: this.t('admin.settings.overview.receipt.noConfig'),
    };
  }

  private deliverySection(cfg: ReturnType<typeof this.config.value>): SettingsSummarySectionVm {
    const d = cfg?.communication?.buyerTicketDelivery;
    const active = this.t('admin.settings.overview.delivery.active');
    const inactive = this.t('admin.settings.overview.delivery.inactive');
    return {
      id: 'delivery',
      icon: 'send',
      title: this.t('admin.settings.overview.delivery.title'),
      route: '/app/admin/company/settings/delivery',
      queryParams: this.linkQueryParams,
      facts: d ? [
        this.badge(this.t('admin.settings.overview.delivery.sms'), d.sms?.enabled ?? false, active, inactive),
        this.badge(this.t('admin.settings.overview.delivery.whatsapp'), d.whatsapp?.enabled ?? false, active, inactive),
        this.badge(this.t('admin.settings.overview.delivery.email'), d.email?.enabled ?? false, active, inactive),
      ] : null,
      emptyMessage: this.t('admin.settings.overview.delivery.noConfig'),
    };
  }

  private calendarSection(cfg: ReturnType<typeof this.config.value>): SettingsSummarySectionVm {
    const cal = cfg?.rules?.businessCalendar;
    const facts: SettingsSummaryFact[] = cal ? [
      this.text(this.t('admin.settings.overview.calendar.defaultOpen'), this.t((cal.defaultOpen ?? true) ? 'common.yes' : 'common.no')),
      ...((cal.closedWeekdays ?? []).length > 0
        ? [this.text(this.t('admin.settings.config.calendar.closedWeekdays'), String((cal.closedWeekdays ?? []).length))]
        : []),
      ...((cal.holidays ?? []).length > 0
        ? [this.text(this.t('admin.settings.config.calendar.recurringHolidays'), String((cal.holidays ?? []).length))]
        : []),
    ] : [];
    return {
      id: 'calendar',
      icon: 'event',
      title: this.t('admin.settings.overview.calendar.title'),
      route: '/app/admin/company/settings/calendar',
      queryParams: this.linkQueryParams,
      facts: cal ? facts : null,
      emptyMessage: this.t('admin.settings.overview.calendar.noConfig'),
    };
  }

  private badge(label: string, active: boolean, activeText: string, inactiveText: string): SettingsSummaryFact {
    return { label, value: active ? activeText : inactiveText, badge: true, active };
  }

  private text(label: string, value: string): SettingsSummaryFact {
    return { label, value, badge: false, active: false };
  }

  private paperSizeLabel(size: string | null): string {
    const map: Record<string, string> = { RECEIPT_58MM: '58 mm', RECEIPT_80MM: '80 mm', A4: 'A4' };
    return size ? (map[size] ?? size) : '80 mm';
  }

  private t(key: string, params?: Record<string, string | number>): string {
    return this.translate.instant(key, params);
  }
}
