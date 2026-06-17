import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import type { TchPage } from '@tch/api';
import type { SellerTerminalStatus } from './seller-terminal-admin.api.service';

export interface CommissionOverview {
  readonly tenantDefaultRate: number;
  readonly totalSellerTerminals: number;
  readonly countAtDefaultRate: number;
  readonly countWithCustomRate: number;
  readonly minRate: number | null;
  readonly maxRate: number | null;
}

export type CommissionRateSource = 'DEFAULT' | 'CUSTOM';

export interface SellerCommissionRow {
  readonly sellerTerminalId: string;
  readonly terminalCode: string;
  readonly displayName: string;
  readonly status: SellerTerminalStatus;
  readonly commissionRate: number;
  readonly rateSource: CommissionRateSource;
}

@Injectable({ providedIn: 'root' })
export class CommissionAdminApi {
  private readonly backend = inject(TchBackendClient);

  overview(): Observable<CommissionOverview> {
    return this.backend.get<CommissionOverview>('/admin/commission/overview');
  }

  setDefaultRate(rate: number): Observable<void> {
    return this.backend.put<void>('/admin/commission/default-rate', { rate });
  }

  listSellers(params: { page?: number; size?: number } = {}): Observable<TchPage<SellerCommissionRow>> {
    const p: Record<string, string> = {};
    if (params.page !== undefined) p['page'] = String(params.page);
    if (params.size !== undefined) p['size'] = String(params.size);
    return this.backend.get<TchPage<SellerCommissionRow>>('/admin/commission/sellers', { params: p });
  }

  setSellerRate(sellerTerminalId: string, rate: number): Observable<void> {
    return this.backend.put<void>(`/admin/commission/sellers/${sellerTerminalId}`, { rate });
  }

  resetSellerRate(sellerTerminalId: string): Observable<void> {
    return this.backend.delete<void>(`/admin/commission/sellers/${sellerTerminalId}/custom-rate`);
  }
}
