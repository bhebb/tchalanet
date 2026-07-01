import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions, appendQuery } from '@tch/api';
import { Observable } from 'rxjs';

export type DrawResultStatus = 'PENDING' | 'APPLIED' | 'CORRECTED' | 'VOIDED';
export type DrawResultQuality = 'OFFICIAL' | 'MANUAL' | 'ESTIMATED' | 'UNKNOWN';

export interface DrawResultView {
  id: string;
  drawId: string;
  drawDate: string;
  slotKey: string;
  slotLabel: string;
  channelCode: string;
  channelName: string;
  gameCode: string;
  status: DrawResultStatus;
  quality: DrawResultQuality;
  numbers: number[];
  occurredAt?: string | null;
  fetchedAt?: string | null;
  appliedAt?: string | null;
  publishedAt?: string | null;
}

export interface ListDrawResultsParams {
  slotKey?: string;
  status?: DrawResultStatus;
  quality?: DrawResultQuality;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
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
