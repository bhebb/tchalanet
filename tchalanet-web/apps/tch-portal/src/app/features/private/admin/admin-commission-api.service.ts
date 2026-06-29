import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface CommissionOverviewView {
  tenantDefaultRate: number;
  totalSellerTerminals: number;
  countAtDefaultRate: number;
  countWithCustomRate: number;
  minRate: number;
  maxRate: number;
}

export type CommissionRateSource = 'DEFAULT' | 'CUSTOM';

export interface SellerTerminalCommissionRow {
  id: { value: string };
  terminalCode: string;
  displayName: string;
  status: string;
  commissionRate: number;
  source: CommissionRateSource;
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
