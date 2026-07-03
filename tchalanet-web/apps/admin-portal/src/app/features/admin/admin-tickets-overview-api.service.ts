import { Injectable, Injector, ResourceRef, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TchRequestOptions } from '@tch/api';
import { forkJoin, map, switchMap } from 'rxjs';

import {
  AdminFinancialsApi,
  TenantFinancialBreakdownView,
} from './financials/data-access/admin-financials-api.service';
import { AdminTicketsApi, TicketRowView } from './admin-tickets-api.service';

export interface AdminTicketsOverviewView {
  readonly financials: TenantFinancialBreakdownView;
  readonly recentTickets: readonly TicketRowView[];
  readonly cancelledCount: number;
}

@Injectable({ providedIn: 'root' })
export class AdminTicketsOverviewApi {
  private readonly financialsApi = inject(AdminFinancialsApi);
  private readonly ticketsApi = inject(AdminTicketsApi);
  private readonly injector = inject(Injector);

  overviewResource(
    refresh: () => number,
    options?: TchRequestOptions,
  ): ResourceRef<AdminTicketsOverviewView | undefined> {
    return rxResource<AdminTicketsOverviewView, number>({
      injector: this.injector,
      params: () => refresh(),
      stream: () =>
        this.financialsApi.getBreakdown({
          drawLimit: 10,
          sellerTerminalLimit: 10,
        }, options).pipe(
          switchMap(financials =>
            forkJoin({
              recent: this.ticketsApi.list({
                fromDate: financials.from,
                toDate: financials.to,
                sort: 'createdAt,DESC',
                page: 0,
                size: 8,
              }, options),
              cancelled: this.ticketsApi.list({
                status: 'CANCELLED',
                fromDate: financials.from,
                toDate: financials.to,
                sort: 'createdAt,DESC',
                page: 0,
                size: 1,
              }, options),
            }).pipe(
              map(({ recent, cancelled }) => ({
                financials,
                recentTickets: recent.items,
                cancelledCount: cancelled.totalElements,
              })),
            ),
          ),
        ),
    });
  }
}
