import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions, appendQuery } from '@tch/api';
import { Observable } from 'rxjs';

export type TicketStatus = 'PLACED' | 'PAID' | 'CANCELLED' | 'EXPIRED' | 'PENDING';

export interface TicketRowView {
  readonly id: string;
  readonly ticketCode: string;
  readonly publicCode: string;
  readonly status: TicketStatus;
  readonly drawId: string;
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
    return this.backend.get<TchPage<TicketRowView>>(
      appendQuery('/tenant/cashier/tickets', {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'placedAt,desc',
        status: params.status,
      }),
      options,
    );
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
