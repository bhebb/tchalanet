import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export type SellerTerminalStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'DISABLED';

export interface SellerTerminalSummaryRow {
  id: { value: string };
  terminalCode: string;
  displayName: string;
  phoneNumber?: string | null;
  status: SellerTerminalStatus;
  commissionRate?: number | null;
  lastSeenAt?: string | null;
  activatedAt?: string | null;
  todayTicketCount?: number | null;
  todaySalesAmount?: number | null;
  pinResetRequired?: boolean | null;
}

export interface SellerTerminalsSummary {
  activeCount: number;
  blockedCount: number;
  salesTodayAmount: number;
  averageCommissionRate: number;
  currency: 'HTG';
}

export interface SellerTerminalView extends SellerTerminalSummaryRow {
  firstName?: string | null;
  lastName?: string | null;
}

export interface AddressRequest {
  line1: string;
  line2?: string | null;
  city: string;
  region?: string | null;
  country: string;
  postalCode?: string | null;
}

export interface CreateSellerTerminalRequest {
  terminalCode: string;
  displayName: string;
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  commissionRate?: number | null;
  initialPin: string;
  active?: boolean;
  address?: AddressRequest | null;
}

export interface CreateSellerTerminalResult {
  sellerTerminalId: string;
  terminalCode: string;
  displayName: string;
  initialPin: string;
  status: 'ACTIVE' | 'BLOCKED' | 'PENDING';
}

export interface UpdateSellerTerminalRequest {
  displayName: string;
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  commissionRate?: number | null;
}

export type PinResetReason =
  | 'PIN_LOST'
  | 'SELLER_CHANGED'
  | 'SUSPECTED_COMPROMISE'
  | 'ADMIN_CORRECTION'
  | 'OTHER';

export interface ResetSellerTerminalPinRequest {
  reason: PinResetReason;
}

export interface ResetSellerTerminalPinResponse {
  sellerTerminalId: string;
  terminalCode: string;
  temporaryPin: string;
  mustChangePin: boolean;
  pinResetAt: string;
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

  getSummary(): Observable<SellerTerminalsSummary> {
    return this.backend.get<SellerTerminalsSummary>('/admin/seller-terminals/summary');
  }

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

  createFull(req: CreateSellerTerminalRequest): Observable<CreateSellerTerminalResult> {
    return this.backend.post<{ value: string }>('/admin/seller-terminals', req).pipe(
      map(res => ({
        sellerTerminalId: res.value,
        terminalCode: req.terminalCode,
        displayName: req.displayName,
        initialPin: req.initialPin,
        // V0: status derived from request until backend returns it on create
        status: (req.active !== false ? 'ACTIVE' : 'PENDING') as 'ACTIVE' | 'BLOCKED' | 'PENDING',
      })),
    );
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

  resetPin(id: string, req: ResetSellerTerminalPinRequest): Observable<ResetSellerTerminalPinResponse> {
    return this.backend.post<ResetSellerTerminalPinResponse>(`/admin/seller-terminals/${id}/pin-reset`, req);
  }
}
