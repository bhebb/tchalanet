import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface SalesReportLine {
  readonly date: string;
  readonly gameCode: string;
  readonly ticketsSold: number;
  readonly totalSales: number;
  readonly totalPayout: number;
  readonly netRevenue: number;
}

export interface SalesReportResponse {
  readonly fromDate: string;
  readonly toDate: string;
  readonly gameCode: string | null;
  readonly lines: SalesReportLine[];
}

@Injectable({ providedIn: 'root' })
export class ReportsAdminApi {
  private readonly backend = inject(TchBackendClient);

  getSalesReport(params: { from?: string; to?: string; gameCode?: string } = {}): Observable<SalesReportResponse> {
    const qp: Record<string, string> = {};
    if (params.from) qp['from'] = params.from;
    if (params.to) qp['to'] = params.to;
    if (params.gameCode) qp['gameCode'] = params.gameCode;
    return this.backend.get<SalesReportResponse>('/tenant/reports/sales-by-period-and-game', { params: qp });
  }
}
