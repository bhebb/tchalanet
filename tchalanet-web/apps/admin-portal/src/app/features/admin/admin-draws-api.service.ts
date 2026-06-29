import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage, appendQuery } from '@tch/api';
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
    return this.backend.get<TchPage<DrawSummaryView>>(appendQuery('/admin/draws', params), options);
  }

  listToday(
    params: { resultSlotId?: string; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawSummaryView>> {
    return this.backend.get<TchPage<DrawSummaryView>>(
      appendQuery('/admin/draws/today', params),
      options,
    );
  }

  listUpcoming(
    params: { resultSlotId?: string; days?: number; page?: number; size?: number } = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<DrawSummaryView>> {
    return this.backend.get<TchPage<DrawSummaryView>>(
      appendQuery('/admin/draws/upcoming', params),
      options,
    );
  }

  get(drawId: string, options?: TchRequestOptions): Observable<DrawSummaryView> {
    return this.backend.get<DrawSummaryView>(`/admin/draws/${drawId}`, options);
  }
}
