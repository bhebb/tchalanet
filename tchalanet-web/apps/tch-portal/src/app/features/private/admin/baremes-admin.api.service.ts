import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface PricingOddsEntry {
  readonly id: string | null;
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds: number;
  readonly active: boolean;
}

export interface OddsOverrideEntry {
  readonly id: string;
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds: number;
  readonly active: boolean;
  readonly effectiveFrom: string | null;
  readonly effectiveTo: string | null;
  readonly reason: string | null;
}

export interface UpsertOverrideRequest {
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly odds: number;
  readonly reason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class BaremesAdminApi {
  private readonly backend = inject(TchBackendClient);

  listTenantOdds(): Observable<PricingOddsEntry[]> {
    return this.backend.get<PricingOddsEntry[]>('/admin/controls/odds');
  }

  listSellerOverrides(sellerTerminalId: string): Observable<OddsOverrideEntry[]> {
    return this.backend.get<OddsOverrideEntry[]>(`/admin/controls/odds/seller-terminals/${sellerTerminalId}`);
  }

  upsertOverride(sellerTerminalId: string, req: UpsertOverrideRequest): Observable<unknown> {
    return this.backend.put<unknown>(`/admin/controls/odds/seller-terminals/${sellerTerminalId}`, req);
  }

  deleteOverride(sellerTerminalId: string, overrideId: string): Observable<void> {
    return this.backend.delete<void>(`/admin/controls/odds/seller-terminals/${sellerTerminalId}/overrides/${overrideId}`);
  }
}
