import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type PayoutStatus = 'PENDING' | 'APPROVED' | 'PAID' | 'BLOCKED' | 'CANCELLED' | 'REVERSED';

export interface PayoutRowView {
  id: string;
  ticketId?: string | null;
  status: PayoutStatus;
  amount: number;
  currency: string;
  sellerTerminalCode?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PayoutDetailsView extends PayoutRowView {
  drawId?: string | null;
  gameCode?: string | null;
  drawDate?: string | null;
  reason?: string | null;
}

export interface TchPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ListPayoutsParams {
  status?: PayoutStatus;
  ticketId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminPayoutsApi {
  private readonly backend = inject(TchBackendClient);

  list(params: ListPayoutsParams = {}): Observable<TchPage<PayoutRowView>> {
    const p = new URLSearchParams();
    if (params.status) p.set('status', params.status);
    if (params.ticketId) p.set('ticketId', params.ticketId);
    if (params.from) p.set('from', params.from);
    if (params.to) p.set('to', params.to);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<PayoutRowView>>(`/admin/payouts${qs ? `?${qs}` : ''}`);
  }

  get(payoutId: string): Observable<PayoutDetailsView> {
    return this.backend.get<PayoutDetailsView>(`/admin/payouts/${payoutId}`);
  }

  block(payoutId: string, reason: string): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/block`, { reason });
  }

  unblock(payoutId: string): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/unblock`, {});
  }

  cancel(payoutId: string, reason: string): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/cancel`, { reason });
  }

  reverse(payoutId: string, reason: string): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/reverse`, { reason });
  }
}
