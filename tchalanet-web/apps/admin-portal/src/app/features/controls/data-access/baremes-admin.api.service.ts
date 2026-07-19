import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface PricingOddsEntry {
  readonly id: string | null;
  readonly gameCode: string;
  readonly pricingVariantCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds: number | null;
  readonly payoutRuleType?: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT' | null;
  readonly fixedAmount?: number | null;
  readonly active: boolean;
}

export interface OddsOverrideEntry {
  readonly id: string | { value?: string | null };
  readonly gameCode: string;
  readonly pricingVariantCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds: number | null;
  readonly payoutRuleType?: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT' | null;
  readonly fixedAmount?: number | null;
  readonly active: boolean;
  readonly effectiveFrom: string | null;
  readonly effectiveTo: string | null;
  readonly reason: string | null;
}

export interface UpsertOverrideRequest {
  readonly gameCode: string;
  readonly pricingVariantCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds?: number | null;
  readonly payoutRuleType?: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT' | null;
  readonly fixedAmount?: number | null;
  readonly reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class BaremesAdminApi {
  private readonly backend = inject(TchBackendClient);

  listTenantOdds(): Observable<PricingOddsEntry[]> {
    return this.backend.get<PricingOddsEntry[]>('/admin/controls/pricing-rules', {
      suppressShellFeedback: true,
    });
  }

  listSellerOverrides(
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<OddsOverrideEntry[]> {
    return this.backend.get<OddsOverrideEntry[]>(
      `/admin/controls/pricing-rules/seller-terminals/${sellerTerminalId}`,
      options,
    );
  }

  upsertOverride(
    sellerTerminalId: string,
    req: UpsertOverrideRequest,
    options?: TchRequestOptions,
  ): Observable<unknown> {
    return this.backend.put<unknown>(
      `/admin/controls/pricing-rules/seller-terminals/${sellerTerminalId}`,
      req,
      options,
    );
  }

  deleteOverride(
    sellerTerminalId: string,
    overrideId: string,
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.delete<void>(
      `/admin/controls/pricing-rules/seller-terminals/${sellerTerminalId}/overrides/${overrideId}`,
      options,
    );
  }
}
