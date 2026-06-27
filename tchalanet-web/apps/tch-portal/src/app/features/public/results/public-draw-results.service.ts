import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface PublicDrawResultRow {
  readonly drawResultId: string;
  readonly slotKey: string;
  readonly provider: string;
  readonly drawChannelLabelKey: string;
  readonly drawChannelLabel: string;
  readonly resultDate: string;
  readonly drawTime: string;
  readonly timezone: string;
  readonly status: string;
  readonly numbers: readonly string[];
  readonly occurredAt: string;
  readonly detailPath: string;
}

export interface PublicDrawResultSlot {
  readonly slotKey: string;
  readonly provider: string;
  readonly label: string;
  readonly timezone: string;
  readonly drawTime: string;
}

export interface PublicDrawResultSlotsResponse {
  readonly items: readonly PublicDrawResultSlot[];
}

export interface PublicDrawResultDetail {
  readonly drawResultId: string;
  readonly slotKey: string;
  readonly provider: string;
  readonly drawChannelLabelKey: string;
  readonly drawChannelLabel: string;
  readonly resultDate: string;
  readonly drawTime: string;
  readonly timezone: string;
  readonly occurredAt: string;
  readonly status: string;
  readonly numbers: readonly string[];
  readonly sourceLabel: string | null;
  readonly publishedAt: string | null;
  readonly nextResultAt: string | null;
}

export interface PublicDrawResultHistoryPage {
  readonly items: readonly PublicDrawResultRow[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

export interface HistoryQueryParams {
  readonly slotKeys?: readonly string[];
  readonly from?: string;
  readonly to?: string;
  readonly page: number;
  readonly size: number;
}

@Injectable({ providedIn: 'root' })
export class PublicDrawResultsService {
  private readonly backend = inject(TchBackendClient);

  slots(): Observable<PublicDrawResultSlotsResponse> {
    return this.backend.get<PublicDrawResultSlotsResponse>('/public/draw-results/slots');
  }

  history(params: HistoryQueryParams): Observable<PublicDrawResultHistoryPage> {
    let p = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size))
      .set('sort', 'occurredAt,desc');

    params.slotKeys?.forEach(k => {
      p = p.append('slotKeys', k);
    });
    if (params.from) p = p.set('from', params.from);
    if (params.to) p = p.set('to', params.to);

    return this.backend.get<PublicDrawResultHistoryPage>('/public/draw-results/history', {
      params: p,
    });
  }

  detail(drawResultId: string): Observable<PublicDrawResultDetail> {
    return this.backend.get<PublicDrawResultDetail>(
      `/public/draw-results/${encodeURIComponent(drawResultId)}`,
    );
  }
}
