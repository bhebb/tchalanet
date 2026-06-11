import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiResponse, unwrapApiResponse } from '@tch/api';

// ── Public row (history table) ────────────────────────────────────────────────
// Shape mirrors PublicDrawResultRow (Java record) from tchalanet-server.

export interface PublicDrawResultRow {
  readonly drawResultId: string;
  readonly slotKey: string;
  readonly provider: string;
  /**
   * Stable i18n key sent by the server (e.g. "draw_channel.ny.eve.label").
   * Use with TranslatePipe; falls back to drawChannelLabel if not found.
   */
  readonly drawChannelLabelKey: string;
  /** Human-readable label sent by the server — used as fallback if i18n key resolves to nothing. */
  readonly drawChannelLabel: string;
  readonly resultDate: string;
  readonly drawTime: string;
  readonly timezone: string;
  readonly status: string;
  readonly numbers: readonly string[];
  readonly occurredAt: string;
  /** Relative path to the detail page: /public/results/{drawResultId} */
  readonly detailPath: string;
}

// ── Public detail ─────────────────────────────────────────────────────────────
// Shape mirrors PublicDrawResultDetailResponse (Java record) from tchalanet-server.

export interface PublicDrawResultDetail {
  readonly drawResultId: string;
  readonly slotKey: string;
  readonly provider: string;
  /** Stable i18n key (e.g. "draw_channel.ny.eve.label"). */
  readonly drawChannelLabelKey: string;
  /** Human-readable fallback label. */
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

// ── History response ──────────────────────────────────────────────────────────
// Mirrors PublicDrawResultHistoryResponse (Java record).
// Note: field is totalItems (not totalElements as in TchPage) — use this type directly.

export interface PublicDrawResultHistoryPage {
  readonly items: readonly PublicDrawResultRow[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
}

// ── Query params ──────────────────────────────────────────────────────────────

export interface HistoryQueryParams {
  readonly slotKeys?: readonly string[];
  readonly from?: string;
  readonly to?: string;
  readonly page: number;
  readonly size: number;
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PublicDrawResultsService {
  private readonly http = inject(HttpClient);

  /**
   * Paginated history for the /public/results table.
   * Maps to GET /api/v1/public/draw-results/history.
   */
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

    return this.http
      .get<ApiResponse<PublicDrawResultHistoryPage>>('/api/v1/public/draw-results/history', {
        params: p,
      })
      .pipe(map(unwrapApiResponse));
  }

  /**
   * Single draw result for the /public/results/:drawResultId detail page.
   * Maps to GET /api/v1/public/draw-results/{drawResultId}.
   */
  detail(drawResultId: string): Observable<PublicDrawResultDetail> {
    return this.http
      .get<ApiResponse<PublicDrawResultDetail>>(
        `/api/v1/public/draw-results/${encodeURIComponent(drawResultId)}`,
      )
      .pipe(map(unwrapApiResponse));
  }
}
