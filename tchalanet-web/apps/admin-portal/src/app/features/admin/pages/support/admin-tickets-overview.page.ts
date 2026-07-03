import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { catchError, forkJoin, of, startWith, switchMap } from 'rxjs';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminFinancialsApi,
  TenantFinancialBreakdownView,
} from '../../financials/data-access/admin-financials-api.service';
import { AdminTicketsApi, TicketRowView } from '../../admin-tickets-api.service';

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly financials: TenantFinancialBreakdownView;
      readonly recentTickets: readonly TicketRowView[];
      readonly cancelledCount: number;
    };

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
    DatePipe,
    DecimalPipe,
    RouterLink,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    MatButtonModule,
    MatIconModule,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './admin-tickets-overview.page.html',
  styleUrls: ['./admin-tickets-overview.page.scss'],
})
export class AdminTicketsOverviewPage {
  private readonly financialsApi = inject(AdminFinancialsApi);
  private readonly ticketsApi = inject(AdminTicketsApi);

  readonly refreshTick = signal(0);

  readonly state = toSignal(
    toObservable(this.refreshTick).pipe(
      switchMap(() =>
        this.financialsApi.getBreakdown({
          drawLimit: 10,
          sellerTerminalLimit: 10,
        }, { suppressShellFeedback: true }).pipe(
          switchMap(financials =>
            forkJoin({
              recent: this.ticketsApi.list({
                fromDate: financials.from,
                toDate: financials.to,
                sort: 'createdAt,DESC',
                page: 0,
                size: 8,
              }, { suppressShellFeedback: true }),
              cancelled: this.ticketsApi.list({
                status: 'CANCELLED',
                fromDate: financials.from,
                toDate: financials.to,
                sort: 'createdAt,DESC',
                page: 0,
                size: 1,
              }, { suppressShellFeedback: true }),
            }).pipe(
              switchMap(({ recent, cancelled }) => of({
                status: 'ready',
                financials,
                recentTickets: recent.items,
                cancelledCount: cancelled.totalElements,
              } as PageState)),
            ),
          ),
          catchError(() => of({ status: 'error' } as PageState)),
          startWith({ status: 'loading' } as PageState),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  readonly summary = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.financials.summary : null;
  });

  readonly ticketAverage = computed(() => {
    const summary = this.summary();
    if (!summary || summary.ticketsSold === 0) return 0;
    return summary.grossSales / summary.ticketsSold;
  });

  readonly drawRows = computed<readonly OverviewRow[]>(() => {
    const state = this.state();
    if (state.status !== 'ready') return [];
    return state.financials.drawRows
      .slice(0, 6)
      .map(row => ({
        id: row.drawId,
        label: this.drawLabel(row.drawChannelCode, row.gameCode),
        ticketCount: row.ticketsSold,
        grossSales: row.grossSales,
        meta: row.scheduledAt,
      }));
  });

  readonly sellerRows = computed<readonly OverviewRow[]>(() => {
    const state = this.state();
    if (state.status !== 'ready') return [];
    return [...state.financials.sellerTerminalDailyRows]
      .sort((a, b) => b.grossSales - a.grossSales)
      .slice(0, 6)
      .map(row => ({
        id: row.sellerTerminalId,
        label: this.shortId(row.sellerTerminalId),
        ticketCount: row.ticketsSold,
        grossSales: row.grossSales,
      }));
  });

  readonly recentTickets = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.recentTickets : [];
  });

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  currency(): string {
    const ticket = this.recentTickets()[0];
    return ticket?.currency ?? DEFAULT_CURRENCY;
  }

  amountDisplay(amount: number): string {
    return amount.toFixed(2);
  }

  ticketAmountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'APPROVED': return 'Valide';
      case 'PENDING_APPROVAL': return 'En attente';
      case 'REJECTED': return 'Rejeté';
      case 'CANCELLED': return 'Annulé';
      case 'VOIDED': return 'Invalidé';
      case 'PAID': return 'Payé';
      case 'EXPIRED': return 'Expiré';
      default: return status;
    }
  }

  private drawLabel(channel: string | null, game: string): string {
    return [channel, game].filter(Boolean).join(' · ') || game || 'Tirage';
  }

  private shortId(id: string): string {
    return id.length <= 8 ? id : id.slice(0, 8);
  }
}
