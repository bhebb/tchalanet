import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type SellerTerminalStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'DISABLED';

export interface SellerTerminalSummaryRow {
  id: { value: string };
  terminalCode: string;
  displayName: string;
  phoneNumber?: string | null;
  status: SellerTerminalStatus;
  commissionRate?: number | null;
  outletId?: { value: string } | null;
  lastSeenAt?: string | null;
  activatedAt?: string | null;
  todayTicketCount?: number | null;
  todaySalesAmount?: number | null;
}

export interface SellerTerminalView extends SellerTerminalSummaryRow {
  firstName?: string | null;
  lastName?: string | null;
}

export interface CreateSellerTerminalRequest {
  terminalCode: string;
  displayName: string;
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  commissionRate?: number | null;
  initialPin: string;
  outletId?: string | null;
}

export interface UpdateSellerTerminalRequest {
  displayName: string;
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  commissionRate?: number | null;
}

export interface TchPage<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface ListSellerTerminalsParams {
  q?: string;
  status?: SellerTerminalStatus | '';
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class SellerTerminalApi {
  private readonly backend = inject(TchBackendClient);

  list(params: ListSellerTerminalsParams = {}): Observable<TchPage<SellerTerminalSummaryRow>> {
    const p = new URLSearchParams();
    if (params.q) p.set('q', params.q);
    if (params.status) p.set('status', params.status);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<SellerTerminalSummaryRow>>(
      `/admin/seller-terminals${qs ? `?${qs}` : ''}`,
    );
  }

  get(id: string): Observable<SellerTerminalView> {
    return this.backend.get<SellerTerminalView>(`/admin/seller-terminals/${id}`);
  }

  create(req: CreateSellerTerminalRequest): Observable<{ value: string }> {
    return this.backend.post<{ value: string }>('/admin/seller-terminals', req);
  }

  update(id: string, req: UpdateSellerTerminalRequest): Observable<void> {
    return this.backend.put<void>(`/admin/seller-terminals/${id}`, req);
  }

  block(id: string, reason: string): Observable<void> {
    return this.backend.patch<void>(`/admin/seller-terminals/${id}/block`, { reason });
  }

  unblock(id: string): Observable<void> {
    return this.backend.patch<void>(`/admin/seller-terminals/${id}/unblock`, {});
  }

  disable(id: string): Observable<void> {
    return this.backend.patch<void>(`/admin/seller-terminals/${id}/disable`, {});
  }

  resetAccess(id: string, newPin: string): Observable<void> {
    return this.backend.patch<void>(`/admin/seller-terminals/${id}/reset-access`, { newPin });
  }
}
