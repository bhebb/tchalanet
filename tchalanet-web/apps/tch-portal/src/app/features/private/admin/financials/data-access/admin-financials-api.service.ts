import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface FinancialSummary {
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly winningsCalculated: number;
  readonly payoutsPaid: number;
  readonly sellerCommission: number;
  readonly buyerCharges: number;
  readonly sellerCharges: number;
  readonly tenantCharges: number;
  readonly waivedCharges: number;
  readonly promotionLines: number;
  readonly promotionPricedLines: number;
  readonly promotionPayoutBase: number;
  readonly promotionPotentialPayout: number;
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface DailyFinancialRow extends FinancialSummary {
  readonly refDate: string;
}

export interface DrawFinancialRow extends FinancialSummary {
  readonly drawId: string;
  readonly refDate: string;
  readonly scheduledAt: string;
  readonly gameCode: string;
  readonly drawChannelCode: string | null;
}

export interface SellerTerminalDailyFinancialRow {
  readonly sellerTerminalId: string;
  readonly refDate: string;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly buyerCharges: number;
  readonly sellerCharges: number;
  readonly tenantCharges: number;
  readonly waivedCharges: number;
  readonly promotionLines: number;
  readonly promotionPricedLines: number;
  readonly promotionPayoutBase: number;
  readonly promotionPotentialPayout: number;
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface SellerTerminalDrawFinancialRow extends FinancialSummary {
  readonly sellerTerminalId: string;
  readonly drawId: string;
  readonly refDate: string;
  readonly scheduledAt: string;
  readonly gameCode: string;
  readonly drawChannelCode: string | null;
}

export interface TenantFinancialBreakdownView {
  readonly from: string;
  readonly to: string;
  readonly summary: FinancialSummary;
  readonly dailyRows: readonly DailyFinancialRow[];
  readonly drawRows: readonly DrawFinancialRow[];
  readonly sellerTerminalDrawRows: readonly SellerTerminalDrawFinancialRow[];
  readonly sellerTerminalDailyRows: readonly SellerTerminalDailyFinancialRow[];
}

@Injectable({ providedIn: 'root' })
export class AdminFinancialsApi {
  private readonly backend = inject(TchBackendClient);

  getBreakdown(params: {
    from: string;
    to: string;
    drawLimit?: number;
    sellerTerminalLimit?: number;
  }): Observable<TenantFinancialBreakdownView> {
    const query: Record<string, string> = {
      from: params.from,
      to: params.to,
      drawLimit: String(params.drawLimit ?? 100),
      sellerTerminalLimit: String(params.sellerTerminalLimit ?? 100),
    };
    return this.backend.get<TenantFinancialBreakdownView>('/admin/financials/breakdown', {
      params: query,
    });
  }
}
