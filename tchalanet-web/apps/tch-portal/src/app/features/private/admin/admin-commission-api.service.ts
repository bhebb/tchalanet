import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
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

  getOverview(): Observable<CommissionOverviewView> {
    return this.backend.get<CommissionOverviewView>('/admin/commission/overview');
  }

  setDefaultRate(rate: number): Observable<void> {
    return this.backend.put<void>('/admin/commission/default-rate', { rate });
  }

  listSellers(page = 0, size = 50): Observable<SellerTerminalCommissionRow[]> {
    return this.backend.get<SellerTerminalCommissionRow[]>(
      `/admin/commission/sellers?page=${page}&size=${size}`,
    );
  }

  setSellerRate(sellerTerminalId: string, rate: number): Observable<void> {
    return this.backend.put<void>(`/admin/commission/sellers/${sellerTerminalId}`, { rate });
  }

  resetSellerRate(sellerTerminalId: string): Observable<void> {
    return this.backend.delete<void>(`/admin/commission/sellers/${sellerTerminalId}/custom-rate`);
  }
}
