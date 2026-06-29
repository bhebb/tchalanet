import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type DrawStatus =
  | 'SCHEDULED'
  | 'OPEN'
  | 'LOCKED'
  | 'PENDING_RESULTS'
  | 'RESULTS_APPLIED'
  | 'SETTLED'
  | 'CANCELLED'
  | 'ARCHIVED';

export interface DrawChannelSummary {
  id: string;
  code: string;
  name: string;
  gameCode: string;
}

export interface ResultSlotSummary {
  id: string;
  key: string;
  label: string;
}

export interface DrawSummaryView {
  id: string;
  channel: DrawChannelSummary;
  slot: ResultSlotSummary;
  drawDate: string;
  scheduledAt: string;
  cutoffAt: string;
  status: DrawStatus;
  next: boolean;
  active: boolean;
}

export interface TchPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ListDrawsParams {
  resultSlotId?: string;
  status?: DrawStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminDrawsApi {
  private readonly backend = inject(TchBackendClient);

  list(params: ListDrawsParams = {}, options?: TchRequestOptions): Observable<TchPage<DrawSummaryView>> {
    const p = new URLSearchParams();
    if (params.resultSlotId) p.set('resultSlotId', params.resultSlotId);
    if (params.status) p.set('status', params.status);
    if (params.from) p.set('from', params.from);
    if (params.to) p.set('to', params.to);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<DrawSummaryView>>(`/admin/draws${qs ? `?${qs}` : ''}`, options);
  }

  listToday(
    params: { resultSlotId?: string; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawSummaryView>> {
    const p = new URLSearchParams();
    if (params.resultSlotId) p.set('resultSlotId', params.resultSlotId);
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<DrawSummaryView>>(`/admin/draws/today${qs ? `?${qs}` : ''}`, options);
  }

  listUpcoming(
    params: { resultSlotId?: string; days?: number; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawSummaryView>> {
    const p = new URLSearchParams();
    if (params.resultSlotId) p.set('resultSlotId', params.resultSlotId);
    if (params.days !== undefined) p.set('days', String(params.days));
    if (params.page !== undefined) p.set('page', String(params.page));
    if (params.size !== undefined) p.set('size', String(params.size));
    const qs = p.toString();
    return this.backend.get<TchPage<DrawSummaryView>>(`/admin/draws/upcoming${qs ? `?${qs}` : ''}`, options);
  }

  get(drawId: string, options?: TchRequestOptions): Observable<DrawSummaryView> {
    return this.backend.get<DrawSummaryView>(`/admin/draws/${drawId}`, options);
  }
}
