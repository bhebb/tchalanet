import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions, appendQuery } from '@tch/api';
import { Observable } from 'rxjs';

export type DrawResultStatus =
  | 'PROVISIONAL'
  | 'CONFIRMED'
  | 'OVERRIDDEN'
  | 'ERROR'
  | 'PENDING'
  | 'APPLIED'
  | 'CORRECTED'
  | 'VOIDED';
export type DrawResultQuality =
  | 'COMPLETE'
  | 'SUSPECT'
  | 'INVALID'
  | 'OFFICIAL'
  | 'MANUAL'
  | 'ESTIMATED'
  | 'UNKNOWN';
export type DrawResultSource =
  | 'SYSTEM'
  | 'AUTO'
  | 'EXTERNAL'
  | 'US_LOTTERY'
  | 'NY_OPEN_DATA'
  | 'FL_APIM'
  | 'MANUAL'
  | 'ADMIN_OVERRIDE';

export interface DrawResultView {
  id: string;
  drawId?: string;
  drawDate?: string;
  resultDate?: string;
  slotKey?: string | null;
  slotLabel?: string | null;
  provider?: string | null;
  timezone?: string | null;
  channelCode?: string | null;
  channelName?: string | null;
  gameCode?: string | null;
  status: DrawResultStatus;
  quality: DrawResultQuality;
  source?: DrawResultSource | string | null;
  numbers?: Array<number | string> | null;
  occurredAt?: string | null;
  fetchedAt?: string | null;
  appliedAt?: string | null;
  publishedAt?: string | null;
  sourceResult?: Record<string, unknown> | null;
  haitiResult?: Record<string, unknown> | null;
  rawPayload?: Record<string, unknown> | null;
}

export interface ListDrawResultsParams {
  slotKey?: string;
  status?: DrawResultStatus;
  quality?: DrawResultQuality;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminDrawResultsApi {
  private readonly backend = inject(TchBackendClient);

  list(params: ListDrawResultsParams = {}, options?: TchRequestOptions): Observable<TchPage<DrawResultView>> {
    return this.backend.get<TchPage<DrawResultView>>(
      appendQuery('/admin/draw-results', params),
      options,
    );
  }

  listToday(
    params: { slotKey?: string; status?: DrawResultStatus; quality?: DrawResultQuality; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawResultView>> {
    return this.backend.get<TchPage<DrawResultView>>(
      appendQuery('/admin/draw-results/today', params),
      options,
    );
  }

  listLastDays(
    days: number,
    params: { slotKey?: string; status?: DrawResultStatus; quality?: DrawResultQuality; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawResultView>> {
    return this.backend.get<TchPage<DrawResultView>>(
      appendQuery('/admin/draw-results/last-days', { days, ...params }),
      options,
    );
  }
}
