import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
} from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';

import {
  SellerTerminalApi,
} from '../../data-access/seller-terminal-api.service';
import {
  AdminFinancialsApi,
  SellerTerminalDailyFinancialRow,
} from '../../../financials/data-access/admin-financials-api.service';
import {
  SellerTerminalDetailFact,
  SellerTerminalDetailFactsCardComponent,
} from '../../components/seller-terminal-detail-facts-card/seller-terminal-detail-facts-card.component';
import { SellerTerminalDetailSummaryCardComponent } from '../../components/seller-terminal-detail-summary-card/seller-terminal-detail-summary-card.component';
import { SellerTerminalTodayStatsCardComponent } from '../../components/seller-terminal-today-stats-card/seller-terminal-today-stats-card.component';
import { SellerTerminalIdentityCardComponent } from '../../components/seller-terminal-identity-card/seller-terminal-identity-card.component';

@Component({
  selector: 'tch-admin-seller-terminal-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    SellerTerminalDetailFactsCardComponent,
    SellerTerminalIdentityCardComponent,
    SellerTerminalDetailSummaryCardComponent,
    SellerTerminalTodayStatsCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-seller-terminal-detail.page.html',
  styleUrls: ['./admin-seller-terminal-detail.page.scss'],
})
export class AdminSellerTerminalDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(SellerTerminalApi);
  private readonly financialsApi = inject(AdminFinancialsApi);
  private readonly translate = inject(TranslateService);

  readonly sellerTerminalId = this.route.snapshot.paramMap.get('sellerTerminalId');
  readonly sellerTerminal = this.api.getResource(
    () => this.sellerTerminalId,
    { suppressShellFeedback: true },
  );
  readonly sellerTerminalError = resourceErrorVm(
    this.sellerTerminal,
    'admin.sellerTerminals.detail',
  );
  readonly terminal = computed(() => this.sellerTerminal.value() ?? null);
  readonly todayFinancials = this.financialsApi.getBreakdownResource(
    () => ({ drawLimit: 1, sellerTerminalLimit: 500 }),
    { suppressShellFeedback: true },
  );
  readonly todayFinancialsError = resourceErrorVm(
    this.todayFinancials,
    'admin.sellerTerminals.detail.today',
  );
  readonly todayStats = computed<SellerTerminalDailyFinancialRow>(() => {
    const id = this.sellerTerminalId;
    if (this.todayFinancials.status() === 'error') {
      return emptyTodayStats(id ?? '');
    }
    const row = this.todayFinancials.value()?.sellerTerminalDailyRows
      .find(item => item.sellerTerminalId === id);
    return row ?? emptyTodayStats(id ?? '');
  });

  readonly title = computed(() => {
    const terminal = this.terminal();
    return terminal?.displayName || terminal?.terminalCode || this.translate.instant('admin.sellerTerminals.detail.title');
  });

  readonly controlFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.field.seller_terminal_code', value: terminal.terminalCode },
      {
        labelKey: 'admin.sellerTerminals.detail.field.commissionRate',
        value: terminal.commissionRate,
        kind: 'percent',
      },
      { labelKey: 'admin.sellerTerminals.detail.field.addressId', value: idValue(terminal.addressId) },
      { labelKey: 'admin.sellerTerminals.detail.field.tenantId', value: idValue(terminal.tenantId) },
    ];
  });

  readonly stateFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.currentStatus',
        valueKey: `admin.sellerTerminals.status.${terminal.status}`,
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.canSell',
        value: terminal.status === 'ACTIVE',
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: !!terminal.mustChangePin,
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.blocked',
        value: terminal.status === 'BLOCKED',
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.disabled',
        value: terminal.status === 'DISABLED',
        kind: 'boolean',
      },
    ];
  });

  readonly securityFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      {
        labelKey: 'admin.sellerTerminals.detail.field.mustChangePin',
        value: !!terminal.mustChangePin,
        kind: 'boolean',
      },
      {
        labelKey: 'admin.sellerTerminals.detail.field.pinResetAt',
        value: terminal.pinResetAt,
        kind: 'date',
      },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedReason', value: terminal.blockedReason },
    ];
  });

  readonly activityFacts = computed<readonly SellerTerminalDetailFact[]>(() => {
    const terminal = this.terminal();
    if (!terminal) return [];
    return [
      { labelKey: 'admin.sellerTerminals.detail.field.activatedAt', value: terminal.activatedAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.lastSeenAt', value: terminal.lastSeenAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.blockedAt', value: terminal.blockedAt, kind: 'date' },
      { labelKey: 'admin.sellerTerminals.detail.field.disabledAt', value: terminal.disabledAt, kind: 'date' },
    ];
  });

  reload(): void {
    this.sellerTerminal.reload();
    this.todayFinancials.reload();
  }

  reloadTodayStats(): void {
    this.todayFinancials.reload();
  }

}

function idValue(value: { value?: string | null } | string | null | undefined): string | null {
  if (!value) return null;
  return typeof value === 'string' ? value : value.value ?? null;
}

function emptyTodayStats(sellerTerminalId: string): SellerTerminalDailyFinancialRow {
  return {
    sellerTerminalId,
    refDate: '',
    ticketsSold: 0,
    grossSales: 0,
    sellerCommission: 0,
    buyerCharges: 0,
    sellerCharges: 0,
    tenantCharges: 0,
    waivedCharges: 0,
    promotionLines: 0,
    promotionPricedLines: 0,
    promotionPayoutBase: 0,
    promotionPotentialPayout: 0,
    netRevenueEstimated: 0,
    netRevenuePaidBasis: 0,
  };
}
