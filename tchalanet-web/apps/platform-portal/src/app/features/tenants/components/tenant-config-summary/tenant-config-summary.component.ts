import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminSectionCardComponent } from '@tch/ui/console';

import {
  TenantBusinessCalendarRules,
  TenantDeliveryChannelConfig,
  TenantInternalSettings,
  TenantReceiptConfig,
} from '../../data-access/platform-tenants-api.service';

@Component({
  selector: 'tch-tenant-config-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, AdminEmptyStateComponent],
  templateUrl: './tenant-config-summary.component.html',
  styleUrls: ['./tenant-config-summary.component.scss'],
})
export class TenantConfigSummaryComponent {
  private readonly translate = inject(TranslateService);

  readonly settings = input<TenantInternalSettings | null | undefined>(null);

  readonly locale = computed(() => this.settings()?.locale);
  readonly communication = computed(() => this.settings()?.communication?.buyerTicketDelivery);
  readonly receipt = computed<TenantReceiptConfig | null | undefined>(() => this.settings()?.document?.receipt);
  readonly calendar = computed<TenantBusinessCalendarRules | null | undefined>(
    () => this.settings()?.rules?.businessCalendar,
  );

  channelLabel(channel: TenantDeliveryChannelConfig | null | undefined): string {
    if (!channel) return this.translate.instant('common.not_available');
    const enabledLabel = channel.enabled
      ? this.translate.instant('common.enabled')
      : this.translate.instant('common.disabled');
    if (!channel.enabled) return enabledLabel;
    const amount = channel.amount != null ? `${channel.amount} ${channel.currency ?? ''}`.trim() : '';
    const paidBy = channel.paidBy
      ? this.translate.instant(`platform.tenants.detail.config.paidBy.${channel.paidBy.toLowerCase()}`)
      : '';
    return [enabledLabel, amount, paidBy].filter(Boolean).join(' · ');
  }

  weekdayLabel(day: string): string {
    const key = `common.weekday.${day.toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated === key ? day : translated;
  }
}
