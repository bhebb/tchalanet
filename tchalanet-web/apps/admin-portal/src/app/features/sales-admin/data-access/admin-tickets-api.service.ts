import { Injectable, ResourceRef, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type TicketStatus =
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'VOIDED';

export interface TicketRowView {
  readonly id: string;
  readonly ticketCode: string;
  readonly publicCode: string;
  readonly status: TicketStatus;
  readonly drawId: string;
  readonly drawChannelCode?: string | null;
  readonly resultSlotKey?: string | null;
  readonly resultProvider?: string | null;
  readonly resultTimezone?: string | null;
  readonly drawChannelName: string;
  readonly drawScheduledAt: string;
  readonly totalAmountCents: number;
  readonly currency: string;
  readonly placedAt: string;
}

export interface AdminTicketLineRequest {
  readonly gameCode: string;
  readonly betType: string;
  readonly selection: string;
  readonly betOption: number;
  readonly stake: number;
}

export interface AdminTicketPreviewRequest {
  readonly terminalId: string;
  readonly drawId: string;
  readonly drawChannelId?: string;
  readonly currency: string;
  readonly lines: AdminTicketLineRequest[];
}

export interface AdminSellTicketRequest extends AdminTicketPreviewRequest {
  readonly promotionChoices?: unknown[];
}

export interface AdminTicketPreviewLine {
  readonly gameCode: string;
  readonly betType: string;
  readonly selection: string;
  readonly betOption: number;
  readonly stake: number;
  readonly odds: number;
  readonly potentialGainCents: number;
}

export interface AdminTicketPreviewView {
  readonly totalAmountCents: number;
  readonly currency: string;
  readonly lines: AdminTicketPreviewLine[];
}

export interface AdminSoldTicketView {
  readonly ticketId: string;
  readonly ticketCode: string;
  readonly publicCode: string;
  readonly totalAmountCents: number;
  readonly currency: string;
  readonly placedAt: string;
}

export interface AdminTicketListParams {
  readonly status?: string;
  readonly q?: string;
  readonly from?: string;
  readonly to?: string;
  readonly fromDate?: string;
  readonly toDate?: string;
  readonly sort?: string;
  readonly page?: number;
  readonly size?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminTicketsApi {
  private readonly backend = inject(TchBackendClient);

  list(
    params: AdminTicketListParams = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<TicketRowView>> {
    return this.backend.getPage<TicketRowView>('/tenant/cashier/tickets', {
      ...options,
      params: ticketListQueryParams(params),
    });
  }

  listResource(
    params: () => AdminTicketListParams,
    options?: TchRequestOptions,
  ): ResourceRef<TchPage<TicketRowView> | undefined> {
    return this.backend.getPageResource<TicketRowView>(() => ({
      path: '/tenant/cashier/tickets',
      options: {
        ...options,
        params: ticketListQueryParams(params()),
      },
    }));
  }

  preview(
    req: AdminTicketPreviewRequest,
    options?: TchRequestOptions,
  ): Observable<AdminTicketPreviewView> {
    return this.backend.post<AdminTicketPreviewView>('/tenant/cashier/tickets/preview', req, options);
  }

  sell(
    req: AdminSellTicketRequest,
    options?: TchRequestOptions,
  ): Observable<AdminSoldTicketView> {
    return this.backend.post<AdminSoldTicketView>('/tenant/cashier/tickets/sell', req, options);
  }
}

function ticketListQueryParams(params: AdminTicketListParams): Record<string, string> {
  return {
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
    sort: params.sort || 'createdAt,DESC',
    ...(params.status ? { status: params.status } : {}),
    ...(params.q ? { q: params.q } : {}),
    ...(params.from ? { from: params.from } : {}),
    ...(params.to ? { to: params.to } : {}),
    ...(params.fromDate ? { fromDate: params.fromDate } : {}),
    ...(params.toDate ? { toDate: params.toDate } : {}),
  };
}
