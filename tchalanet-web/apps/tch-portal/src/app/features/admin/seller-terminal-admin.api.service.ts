import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import type { TchPage } from '@tch/api';

export type SellerTerminalStatus = 'ACTIVE' | 'PENDING' | 'BLOCKED' | 'DISABLED';

export interface SellerTerminalRow {
  readonly id: string;
  readonly tenantId: string;
  readonly terminalCode: string;
  readonly displayName: string;
  readonly phoneNumber: string | null;
  readonly status: SellerTerminalStatus;
  readonly commissionRate: number | null;
  readonly outletId: string | null;
  readonly lastSeenAt: string | null;
  readonly activatedAt: string | null;
  readonly todayTicketCount: number | null;
  readonly todaySalesAmount: number | null;
  readonly todayCommissionAmount: number | null;
  readonly lastSaleAt: string | null;
}

export interface CreateSellerTerminalRequest {
  readonly terminalCode: string;
  readonly displayName: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly phoneNumber?: string;
  readonly commissionRate?: number;
  readonly initialPin: string;
  readonly outletId?: string;
  readonly addressId?: string;
}

export interface UpdateSellerTerminalRequest {
  readonly displayName?: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly phoneNumber?: string;
  readonly commissionRate?: number;
  readonly outletId?: string;
  readonly addressId?: string;
}

export interface SellerTerminalListParams {
  readonly q?: string;
  readonly status?: SellerTerminalStatus | null;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
}

@Injectable({ providedIn: 'root' })
export class SellerTerminalAdminApi {
  private readonly backend = inject(TchBackendClient);

  list(params: SellerTerminalListParams = {}): Observable<TchPage<SellerTerminalRow>> {
    const p: Record<string, string> = {};
    if (params.q) p['q'] = params.q;
    if (params.status) p['status'] = params.status;
    if (params.page !== undefined) p['page'] = String(params.page);
    if (params.size !== undefined) p['size'] = String(params.size);
    if (params.sort) p['sort'] = params.sort;
    return this.backend.get<TchPage<SellerTerminalRow>>('/admin/seller-terminals', { params: p });
  }

  getById(id: string): Observable<SellerTerminalRow> {
    return this.backend.get<SellerTerminalRow>(`/admin/seller-terminals/${id}`);
  }

  create(request: CreateSellerTerminalRequest): Observable<string> {
    return this.backend.post<string>('/admin/seller-terminals', request);
  }

  update(id: string, request: UpdateSellerTerminalRequest): Observable<void> {
    return this.backend.put<void>(`/admin/seller-terminals/${id}`, request);
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

  resetPin(id: string, newPin: string): Observable<void> {
    return this.backend.patch<void>(`/admin/seller-terminals/${id}/reset-access`, { newPin });
  }
}
