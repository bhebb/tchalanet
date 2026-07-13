import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { consoleDrawIdentity } from '@tch/web/console';
import { Observable, map } from 'rxjs';

interface ReportDrawLookupDto {
  readonly id: string;
  readonly channel: { readonly code: string; readonly name: string };
  readonly slot: {
    readonly key: string;
    readonly label: string | null;
    readonly timezone: string | null;
    readonly drawTime: string | null;
  };
  readonly drawDate: string;
  readonly scheduledAt: string;
  readonly status: string;
}

export interface ReportDrawSearchOption {
  readonly id: string;
  readonly title: string;
  readonly subtitle?: string;
  readonly badge?: string;
  readonly icon?: string;
}

interface ReportSellerTerminalLookupDto {
  readonly id: { readonly value: string };
  readonly terminalCode: string;
  readonly displayName: string;
  readonly status: string;
}

export interface ReportSellerTerminalSearchOption {
  readonly id: string;
  readonly title: string;
  readonly subtitle?: string;
  readonly badge?: string;
  readonly icon?: string;
}

export interface AdminReportSummary {
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
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface AdminReportDailyRow {
  readonly refDate: string;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly winningsCalculated: number;
  readonly payoutsPaid: number;
  readonly sellerCommission: number;
  readonly tenantCharges: number;
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface AdminReportDrawRow {
  readonly drawId: string;
  readonly refDate: string;
  readonly scheduledAt: string;
  readonly gameCode: string;
  readonly drawChannelCode: string | null;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly winningsCalculated: number;
  readonly payoutsPaid: number;
  readonly sellerCommission: number;
  readonly tenantCharges: number;
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface AdminReportSellerTerminalRow {
  readonly sellerTerminalId: string;
  readonly terminalCode: string | null;
  readonly displayName: string | null;
  readonly status: string | null;
  readonly refDate: string;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly buyerCharges: number;
  readonly sellerCharges: number;
  readonly tenantCharges: number;
  readonly waivedCharges: number;
  readonly drawCount: number;
  readonly averageGrossSalesPerDraw: number;
  readonly netRevenueEstimated: number;
  readonly netRevenuePaidBasis: number;
}

export interface AdminReportTopSelectionRow {
  readonly rank: number;
  readonly displaySelection: string;
  readonly gameCode: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly lineCount: number;
  readonly totalStake: number;
}

export interface AdminReportOverview {
  readonly from: string;
  readonly to: string;
  readonly summary: AdminReportSummary;
  readonly dailyRows: readonly AdminReportDailyRow[];
  readonly drawRows: readonly AdminReportDrawRow[];
  readonly sellerTerminalRows: readonly AdminReportSellerTerminalRow[];
  readonly topSelections: readonly AdminReportTopSelectionRow[];
}

export interface AdminReportDraws {
  readonly from: string;
  readonly to: string;
  readonly summary: AdminReportSummary;
  readonly rows: readonly AdminReportDrawRow[];
}

export interface AdminReportSellerTerminals {
  readonly from: string;
  readonly to: string;
  readonly summary: AdminReportSummary;
  readonly rows: readonly AdminReportSellerTerminalRow[];
}

export interface AdminReportQuery {
  readonly from?: string;
  readonly to?: string;
  readonly drawLimit?: number;
  readonly sellerTerminalLimit?: number;
  readonly drawIds?: readonly string[];
  readonly sellerTerminalIds?: readonly string[];
}

@Injectable({ providedIn: 'root' })
export class AdminReportsApi {
  private readonly backend = inject(TchBackendClient);

  overviewResource(params: () => AdminReportQuery, options?: TchRequestOptions) {
    return this.backend.getResource<AdminReportOverview>(() => ({
      path: '/admin/reports/overview',
      options: {
        ...options,
        params: this.queryParams(params()),
      },
    }));
  }

  drawsResource(params: () => AdminReportQuery, options?: TchRequestOptions) {
    return this.backend.getResource<AdminReportDraws>(() => ({
      path: '/admin/reports/draws',
      options: {
        ...options,
        params: this.limitedQueryParams(params(), 'drawLimit'),
      },
    }));
  }

  sellerTerminalsResource(params: () => AdminReportQuery, options?: TchRequestOptions) {
    return this.backend.getResource<AdminReportSellerTerminals>(() => ({
      path: '/admin/reports/seller-terminals',
      options: {
        ...options,
        params: this.limitedQueryParams(params(), 'sellerTerminalLimit'),
      },
    }));
  }

  searchDraws(query: string, options?: TchRequestOptions): Observable<readonly ReportDrawSearchOption[]> {
    return this.backend.getPage<ReportDrawLookupDto>('/admin/draws', {
      ...options,
      params: {
        q: query,
        page: '0',
        size: '10',
      },
    }).pipe(
      map(page => page.items.map(draw => this.drawOption(draw))),
    );
  }

  searchSellerTerminals(
    query: string,
    options?: TchRequestOptions,
  ): Observable<readonly ReportSellerTerminalSearchOption[]> {
    return this.backend.getPage<ReportSellerTerminalLookupDto>('/admin/seller-terminals', {
      ...options,
      params: {
        q: query,
        page: '0',
        size: '10',
      },
    }).pipe(
      map(page => page.items.map(row => ({
        id: row.id.value,
        title: row.displayName,
        subtitle: row.terminalCode,
        badge: row.status,
        icon: 'point_of_sale',
      }))),
    );
  }

  private queryParams(params: AdminReportQuery): Record<string, string> {
    return {
      drawLimit: String(params.drawLimit ?? 10),
      sellerTerminalLimit: String(params.sellerTerminalLimit ?? 10),
      ...(params.from ? { from: params.from } : {}),
      ...(params.to ? { to: params.to } : {}),
      ...(params.drawIds?.length ? { drawIds: params.drawIds.join(',') } : {}),
      ...(params.sellerTerminalIds?.length ? { sellerTerminalIds: params.sellerTerminalIds.join(',') } : {}),
    };
  }

  private limitedQueryParams(
    params: AdminReportQuery,
    limitSource: 'drawLimit' | 'sellerTerminalLimit',
  ): Record<string, string> {
    return {
      limit: String(params[limitSource] ?? 100),
      ...(params.from ? { from: params.from } : {}),
      ...(params.to ? { to: params.to } : {}),
      ...(params.drawIds?.length ? { drawIds: params.drawIds.join(',') } : {}),
      ...(params.sellerTerminalIds?.length ? { sellerTerminalIds: params.sellerTerminalIds.join(',') } : {}),
    };
  }

  private drawOption(draw: ReportDrawLookupDto): ReportDrawSearchOption {
    const identity = consoleDrawIdentity({
      channelCode: draw.channel.code,
      channelName: draw.channel.name,
      slotKey: draw.slot.key,
      slotLabel: draw.slot.label,
      officialDateLabel: draw.drawDate,
      officialTimeLabel: draw.slot.drawTime,
      officialTimezoneLabel: draw.slot.timezone,
      fallbackTitle: draw.channel.name,
    });
    const title = identity.channelName ?? draw.channel.name;
    const subtitle = [draw.drawDate, identity.slotLabel ?? draw.slot.label, draw.slot.timezone]
      .filter(Boolean)
      .join(' - ');
    return {
      id: draw.id,
      title,
      subtitle,
      badge: draw.status,
      icon: 'event',
    };
  }
}
