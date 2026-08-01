import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface CommissionOverviewView {
  tenantDefaultRate: number | null;
  totalSellerTerminals: number;
  countAtDefaultRate: number;
  countWithCustomRate: number;
  minRate: number | null;
  maxRate: number | null;
}

export type CommissionRateSource = 'DEFAULT' | 'CUSTOM';

export type SerializedSellerTerminalId = { value: string } | string;

export interface SellerTerminalCommissionRow {
  /** The API names the typed-id field sellerTerminalId and serializes it as a UUID string. */
  sellerTerminalId?: SerializedSellerTerminalId;
  /** Compatibility with the previous frontend DTO shape. */
  id?: SerializedSellerTerminalId;
  terminalCode: string;
  displayName: string;
  status: string;
  commissionRate: number;
  rateSource: CommissionRateSource;
}

export function sellerTerminalCommissionId(row: SellerTerminalCommissionRow): string {
  const rawId = row.sellerTerminalId ?? row.id;
  if (!rawId) throw new Error('Seller terminal commission row has no seller terminal id');
  return typeof rawId === 'string' ? rawId : rawId.value;
}

@Injectable({ providedIn: 'root' })
export class AdminCommissionApi {
  private readonly backend = inject(TchBackendClient);

  getOverview(options?: TchRequestOptions): Observable<CommissionOverviewView> {
    return this.backend.get<CommissionOverviewView>('/admin/commission/overview', options);
  }

  setDefaultRate(rate: number, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/commission/default-rate', { rate }, options);
  }

  listSellers(
    page = 0,
    size = 50,
    options?: TchRequestOptions,
  ): Observable<SellerTerminalCommissionRow[]> {
    return this.backend.get<SellerTerminalCommissionRow[]>(
      `/admin/commission/sellers?page=${page}&size=${size}`,
      options,
    );
  }

  setSellerRate(
    sellerTerminalId: string,
    rate: number,
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.put<void>(`/admin/commission/sellers/${sellerTerminalId}`, { rate }, options);
  }

  resetSellerRate(sellerTerminalId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>(`/admin/commission/sellers/${sellerTerminalId}/custom-rate`, options);
  }
}
