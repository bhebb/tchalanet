import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
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

  getDefaultOdds(options?: TchRequestOptions): Observable<PricingView[]> {
    return this.backend.get<PricingView[]>('/admin/controls/odds', options);
  }

  getTerminalOverrides(sellerTerminalId: string, options?: TchRequestOptions): Observable<SellerTerminalOddsOverrideView[]> {
    return this.backend.get<SellerTerminalOddsOverrideView[]>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}`,
      options,
    );
  }

  upsertTerminalOverride(
    sellerTerminalId: string,
    req: UpsertOddsOverrideRequest,
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.put<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}`,
      req,
      options,
    );
  }

  deleteOverride(sellerTerminalId: string, overrideId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}/overrides/${overrideId}`,
      options,
    );
  }

  deactivateOverride(sellerTerminalId: string, overrideId: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(
      `/admin/controls/odds/seller-terminals/${sellerTerminalId}/overrides/${overrideId}/deactivate`,
      {},
      options,
    );
  }
}
