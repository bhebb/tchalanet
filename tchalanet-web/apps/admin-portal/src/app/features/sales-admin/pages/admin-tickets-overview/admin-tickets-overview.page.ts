import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { TchDrawLabel } from '@tch/ui/components';
import { consoleTicketDrawIdentity } from '@tch/web/console';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminMetricCardComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  AdminTicketsOverviewApi,
  AdminTicketsOverviewView,
} from '../../data-access/admin-tickets-overview-api.service';
import { TicketRowView } from '../../data-access/admin-tickets-api.service';
import {
  TenantFinancialBreakdownView,
} from '../../../reports/data-access/admin-financials-api.service';
import { ticketStatusLabelKey } from '../../../../shared/ticket/admin-ticket-status.util';

interface OverviewRow {
  readonly id: string;
  readonly label: string;
  readonly ticketCount: number;
  readonly grossSales: number;
  readonly meta?: string;
}

const DEFAULT_CURRENCY = 'HTG';

@Component({
  selector: 'tch-admin-tickets-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminMetricCardComponent,
    DatePipe,
    DecimalPipe,
    RouterLink,
    TranslatePipe,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchDrawLabel,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-tickets-overview.page.html',
  styleUrls: ['./admin-tickets-overview.page.scss'],
})
export class AdminTicketsOverviewPage {
  private readonly api = inject(AdminTicketsOverviewApi);

  readonly refreshTick = signal(0);
  readonly overview = this.api.overviewResource(
    () => this.refreshTick(),
    { suppressShellFeedback: true },
  );
  readonly overviewError = resourceErrorVm(this.overview, 'admin.tickets.overview');

  ticketAverage(vm: AdminTicketsOverviewView): number {
    const summary = vm.financials.summary;
    if (!summary || summary.ticketsSold === 0) return 0;
    return summary.grossSales / summary.ticketsSold;
  }

  drawRows(financials: TenantFinancialBreakdownView): readonly OverviewRow[] {
    return financials.drawRows
      .slice(0, 6)
      .map(row => ({
        id: row.drawId,
        label: this.drawLabel(row.drawChannelCode, row.gameCode),
        ticketCount: row.ticketsSold,
        grossSales: row.grossSales,
        meta: row.scheduledAt,
      }));
  }

  sellerRows(financials: TenantFinancialBreakdownView): readonly OverviewRow[] {
    return [...financials.sellerTerminalDailyRows]
      .sort((a, b) => b.grossSales - a.grossSales)
      .slice(0, 6)
      .map(row => ({
        id: row.sellerTerminalId,
        label: this.shortId(row.sellerTerminalId),
        ticketCount: row.ticketsSold,
        grossSales: row.grossSales,
      }));
  }

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  currency(vm: AdminTicketsOverviewView): string {
    const ticket = vm.recentTickets[0];
    return ticket?.currency ?? DEFAULT_CURRENCY;
  }

  amountDisplay(amount: number): string {
    return amount.toFixed(2);
  }

  ticketAmountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  ticketDrawIdentity(ticket: TicketRowView) {
    return consoleTicketDrawIdentity({
      channelCode: ticket.drawChannelCode,
      channelLabel: ticket.drawChannelName,
      resultSlotKey: ticket.resultSlotKey,
      scheduledAt: ticket.drawScheduledAt,
      fallbackLabel: ticket.drawChannelName,
    });
  }

  ticketDrawLabel(ticket: TicketRowView): string {
    return this.ticketDrawIdentity(ticket).receiptLabel;
  }

  statusLabelKey(status: string): string {
    return ticketStatusLabelKey(status);
  }

  /** `game` is null for a draw resulted without a sale — show the channel alone. */
  private drawLabel(channel: string | null, game: string | null): string {
    return [channel, game].filter(Boolean).join(' · ') || 'Tirage';
  }

  private shortId(id: string): string {
    return id.length <= 8 ? id : id.slice(0, 8);
  }
}
