import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import {
  GeneratedDrawView,
  GeneratedDrawsQuery,
  PagedResult,
  PlanNextDrawsRequest,
  PlanNextDrawsResult,
  SaveDrawResultRequest,
} from './admin-generated-draws.models';

// TODO(backend): replace mock with real HTTP calls to /admin/draws once endpoint is ready.
const MOCK_DRAWS: GeneratedDrawView[] = [
  // ── 2026-06-23 (today) ──────────────────────────────────────────────────────
  {
    drawId: 'draw-001',
    drawChannelId: 'ch-ny-evening',
    providerCode: 'NY',
    providerLabel: 'New York',
    slotKey: 'EVENING',
    slotLabel: 'Evening',
    label: 'NY Evening',
    businessDate: '2026-06-23',
    scheduledAt: '2026-06-23T22:30:00',
    timezone: 'America/New_York',
    salesStatus: 'OPEN',
    resultStatus: 'NOT_DUE',
    resultMode: 'AUTO',
    publicationStatus: 'NOT_PUBLISHED',
    numbers: null,
    sourceError: null,
  },
  {
    drawId: 'draw-002',
    drawChannelId: 'ch-fl-midday',
    providerCode: 'FL',
    providerLabel: 'Florida',
    slotKey: 'MIDDAY',
    slotLabel: 'Midday',
    label: 'FL Midday',
    businessDate: '2026-06-23',
    scheduledAt: '2026-06-23T13:30:00',
    timezone: 'America/New_York',
    salesStatus: 'CLOSED',
    resultStatus: 'MISSING',
    resultMode: 'MANUAL',
    publicationStatus: 'NOT_PUBLISHED',
    numbers: null,
    sourceError: null,
  },
  {
    drawId: 'draw-003',
    drawChannelId: 'ch-tx-day',
    providerCode: 'TX',
    providerLabel: 'Texas',
    slotKey: 'DAY',
    slotLabel: 'Day',
    label: 'TX Day',
    businessDate: '2026-06-23',
    scheduledAt: '2026-06-23T12:27:00',
    timezone: 'America/Chicago',
    salesStatus: 'CLOSED',
    resultStatus: 'SOURCE_ERROR',
    resultMode: 'AUTO',
    publicationStatus: 'NOT_PUBLISHED',
    numbers: null,
    sourceError: { message: 'Connection timeout', occurredAt: '2026-06-23T12:29:00' },
  },
  {
    drawId: 'draw-004',
    drawChannelId: 'ch-ny-midday',
    providerCode: 'NY',
    providerLabel: 'New York',
    slotKey: 'MIDDAY',
    slotLabel: 'Midday',
    label: 'NY Midday',
    businessDate: '2026-06-23',
    scheduledAt: '2026-06-23T14:30:00',
    timezone: 'America/New_York',
    salesStatus: 'CLOSED',
    resultStatus: 'CONFIRMED',
    resultMode: 'AUTO',
    publicationStatus: 'PUBLISHED',
    numbers: ['21', '08', '14'],
    sourceError: null,
  },
  // ── 2026-06-24 (tomorrow) ───────────────────────────────────────────────────
  {
    drawId: 'draw-005',
    drawChannelId: 'ch-ga-evening',
    providerCode: 'GA',
    providerLabel: 'Georgia',
    slotKey: 'EVENING',
    slotLabel: 'Evening',
    label: 'GA Evening',
    businessDate: '2026-06-24',
    scheduledAt: '2026-06-24T23:00:00',
    timezone: 'America/New_York',
    salesStatus: 'UPCOMING',
    resultStatus: 'NOT_DUE',
    resultMode: 'AUTO',
    publicationStatus: 'NOT_PUBLISHED',
    numbers: null,
    sourceError: null,
  },
  {
    drawId: 'draw-006',
    drawChannelId: 'ch-fl-evening',
    providerCode: 'FL',
    providerLabel: 'Florida',
    slotKey: 'EVENING',
    slotLabel: 'Evening',
    label: 'FL Evening',
    businessDate: '2026-06-24',
    scheduledAt: '2026-06-24T21:00:00',
    timezone: 'America/New_York',
    salesStatus: 'UPCOMING',
    resultStatus: 'NOT_DUE',
    resultMode: 'MANUAL',
    publicationStatus: 'NOT_PUBLISHED',
    numbers: null,
    sourceError: null,
  },
];

@Injectable({ providedIn: 'root' })
export class AdminGeneratedDrawsApiService {
  getGeneratedDraws(params: GeneratedDrawsQuery = {}): Observable<PagedResult<GeneratedDrawView>> {
    let draws = [...MOCK_DRAWS];

    if (params.datePreset === 'TODAY') {
      draws = draws.filter(d => d.businessDate === '2026-06-23');
    } else if (params.datePreset === 'TOMORROW') {
      draws = draws.filter(d => d.businessDate === '2026-06-24');
    }

    if (params.q) {
      const q = params.q.toLowerCase();
      draws = draws.filter(d =>
        d.providerLabel.toLowerCase().includes(q) ||
        d.slotLabel.toLowerCase().includes(q) ||
        d.label.toLowerCase().includes(q),
      );
    }

    if (params.status && params.status !== 'all') {
      draws = draws.filter(d => {
        if (params.status === 'OPEN')        return d.salesStatus === 'OPEN';
        if (params.status === 'EXPECTED')    return d.resultStatus === 'EXPECTED';
        if (params.status === 'MISSING')     return d.resultStatus === 'MISSING';
        if (params.status === 'CONFIRMED')   return d.resultStatus === 'CONFIRMED';
        if (params.status === 'SOURCE_ERROR') return d.resultStatus === 'SOURCE_ERROR';
        return true;
      });
    }

    return of({
      content: draws,
      totalElements: 24,
      page: params.page ?? 0,
      size: params.size ?? 20,
    }).pipe(delay(350));
  }

  planNextDraws(request: PlanNextDrawsRequest = {}): Observable<PlanNextDrawsResult> {
    // TODO(backend): POST /admin/draws/plan
    return of({
      createdCount: 12,
      skippedCount: 2,
      rangeStart: '2026-06-24',
      rangeEnd: '2026-06-30',
    }).pipe(delay(600));
  }

  saveDrawResult(request: SaveDrawResultRequest): Observable<GeneratedDrawView> {
    // TODO(backend): POST /admin/draws/{drawId}/result
    const original = MOCK_DRAWS.find(d => d.drawId === request.drawId);
    const updated: GeneratedDrawView = {
      ...(original ?? MOCK_DRAWS[0]),
      numbers: [...request.numbers],
      resultStatus: request.mode === 'confirmed' ? 'CONFIRMED' : 'PROVISIONAL',
      resultMode: 'MANUAL',
      publicationStatus: request.mode === 'confirmed' ? 'PUBLISHED' : 'NOT_PUBLISHED',
    };
    return of(updated).pipe(delay(700));
  }
}
