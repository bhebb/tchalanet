import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
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
  appliedAt?: string | null;
  publishedAt?: string | null;
}

export interface TchPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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
    const p = new URLSearchParams();
    if (params.slotKey) p.set('slotKey', params.slotKey);
    if (params.status) p.set('status', params.status);
    if (params.quality) p.set('quality', params.quality);
    if (params.from) p.set('from', params.from);
    if (params.to) p.set('to', params.to);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<DrawResultView>>(`/admin/draw-results${qs ? `?${qs}` : ''}`, options);
  }

  listToday(
    params: { slotKey?: string; status?: DrawResultStatus; quality?: DrawResultQuality; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawResultView>> {
    const p = new URLSearchParams();
    if (params.slotKey) p.set('slotKey', params.slotKey);
    if (params.status) p.set('status', params.status);
    if (params.quality) p.set('quality', params.quality);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<DrawResultView>>(`/admin/draw-results/today${qs ? `?${qs}` : ''}`, options);
  }

  listLastDays(
    days: number,
    params: { slotKey?: string; status?: DrawResultStatus; quality?: DrawResultQuality; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawResultView>> {
    const p = new URLSearchParams({ days: String(days) });
    if (params.slotKey) p.set('slotKey', params.slotKey);
    if (params.status) p.set('status', params.status);
    if (params.quality) p.set('quality', params.quality);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    return this.backend.get<TchPage<DrawResultView>>(`/admin/draw-results/last-days?${p}`, options);
  }
}
