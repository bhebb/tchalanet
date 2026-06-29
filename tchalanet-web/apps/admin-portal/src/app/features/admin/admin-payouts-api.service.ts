import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage, appendQuery } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
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

  list(params: ListPayoutsParams = {}, options?: TchRequestOptions): Observable<TchPage<PayoutRowView>> {
    return this.backend.get<TchPage<PayoutRowView>>(appendQuery('/admin/payouts', params), options);
  }

  get(payoutId: string, options?: TchRequestOptions): Observable<PayoutDetailsView> {
    return this.backend.get<PayoutDetailsView>(`/admin/payouts/${payoutId}`, options);
  }

  block(payoutId: string, reason: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/block`, { reason }, options);
  }

  unblock(payoutId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/unblock`, {}, options);
  }

  cancel(payoutId: string, reason: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/cancel`, { reason }, options);
  }

  reverse(payoutId: string, reason: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/payouts/${payoutId}/reverse`, { reason }, options);
  }
}
