import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

// ---- Draw channel models (from /tenant/draw-channels) ----

export interface DrawChannelSummary {
  readonly channelCode: string;
  readonly channelName: string;
  readonly drawTime: string;
  readonly cutoffTime: string;
  readonly timezone: string;
  readonly active: boolean;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface DrawChannelDetail {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly label: string | null;
  readonly timezone: string;
  readonly drawTime: string;
  readonly cutoffSec: number;
  readonly daysOfWeek: DayOfWeek[];
  readonly active: boolean;
  readonly sortOrder: number;
  readonly period: string | null;
  readonly notes: string | null;
}

export interface GameSummary {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly active: boolean;
  readonly sortOrder: number;
}

export interface ChannelGames {
  readonly channelCode: string;
  readonly games: GameSummary[];
}

// ---- Draw models (from /admin/draws) ----

export type DrawStatus =
  | 'SCHEDULED'
  | 'OPEN'
  | 'CLOSED'
  | 'RESULTED'
  | 'SETTLED'
  | 'CANCELED'
  | 'ARCHIVED';

export interface DrawResultSummary {
  readonly id: string;
  readonly occurredAt: string;
  readonly status: string;
  readonly lot1: string | null;
  readonly lot2: string | null;
  readonly lot3: string | null;
  readonly lot4: string | null;
}

export interface DrawSummary {
  readonly id: string;
  readonly channel: { readonly id: string; readonly name: string; readonly code: string };
  readonly slot: {
    readonly id: string;
    readonly key: string;
    readonly label: string;
    readonly timezone: string;
    readonly drawTime: string;
  };
  readonly drawDate: string;
  readonly scheduledAt: string;
  readonly cutoffAt: string;
  readonly status: DrawStatus;
  readonly next: boolean;
  readonly active: boolean;
  readonly lastResult: DrawResultSummary | null;
}

export interface DrawListParams {
  readonly status?: string;
  readonly from?: string;
  readonly to?: string;
  readonly page?: number;
  readonly size?: number;
}

export interface ProposeManualResultRequest {
  readonly drawDate: string;
  readonly slotKey: string;
  readonly pick3?: string;
  readonly pick4?: string;
  readonly notes?: string;
}

@Injectable({ providedIn: 'root' })
export class DrawAdminApi {
  private readonly backend = inject(TchBackendClient);

  // ---- Channels ----

  listChannels(options?: TchRequestOptions): Observable<DrawChannelSummary[]> {
    return this.backend.get<DrawChannelSummary[]>('/tenant/draw-channels', options);
  }

  getChannelByCode(code: string): Observable<DrawChannelDetail> {
    return this.backend.get<DrawChannelDetail>(`/tenant/draw-channels/by-code/${code}`);
  }

  getChannelGames(): Observable<ChannelGames[]> {
    return this.backend.get<ChannelGames[]>('/tenant/draw-channels/games');
  }

  // ---- Draws ----

  listDraws(params: DrawListParams = {}): Observable<TchPage<DrawSummary>> {
    const qp: Record<string, string> = {};
    if (params.status) qp['status'] = params.status;
    if (params.from) qp['from'] = params.from;
    if (params.to) qp['to'] = params.to;
    if (params.page != null) qp['page'] = String(params.page);
    if (params.size != null) qp['size'] = String(params.size);
    return this.backend.get<TchPage<DrawSummary>>('/admin/draws', { params: qp });
  }

  listUpcoming(days = 14): Observable<TchPage<DrawSummary>> {
    return this.backend.get<TchPage<DrawSummary>>('/admin/draws/upcoming', {
      params: { days: String(days), size: '50' },
    });
  }

  getDrawById(id: string, options?: TchRequestOptions): Observable<DrawSummary> {
    return this.backend.get<DrawSummary>(`/admin/draws/${id}`, options);
  }

  // ---- Results ----

  proposeManualResult(req: ProposeManualResultRequest, options?: TchRequestOptions): Observable<unknown> {
    return this.backend.post<unknown>('/admin/draw-results/manual', req, options);
  }
}
