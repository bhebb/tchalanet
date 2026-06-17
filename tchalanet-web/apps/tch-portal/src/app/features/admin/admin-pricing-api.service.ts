import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface PricingView {
  gameCode: string;
  betType: string;
  betOption: number;
  odds: number;
  active: boolean;
}

export interface SellerTerminalOddsOverrideView {
  id: string;
  sellerTerminalId: string;
  gameCode: string;
  betType: string;
  betOption: number;
  odds: number;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  reason?: string | null;
  active: boolean;
}

export interface UpsertOddsOverrideRequest {
  gameCode: string;
  betType: string;
  betOption: number;
  odds: number;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminPricingApi {
  private readonly backend = inject(TchBackendClient);

  getDefaultOdds(): Observable<PricingView[]> {
    return this.backend.get<PricingView[]>('/admin/controls/odds');
  }

  getTerminalOverrides(sellerTerminalId: string): Observable<SellerTerminalOddsOverrideView[]> {
    return this.backend.get<SellerTerminalOddsOverrideView[]>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}`,
    );
  }

  upsertTerminalOverride(
    sellerTerminalId: string,
    req: UpsertOddsOverrideRequest,
  ): Observable<void> {
    return this.backend.put<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}`,
      req,
    );
  }

  deleteOverride(sellerTerminalId: string, overrideId: string): Observable<void> {
    return this.backend.delete<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}/overrides/${overrideId}`,
    );
  }

  deactivateOverride(sellerTerminalId: string, overrideId: string): Observable<void> {
    return this.backend.post<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}/overrides/${overrideId}/deactivate`,
      {},
    );
  }
}
