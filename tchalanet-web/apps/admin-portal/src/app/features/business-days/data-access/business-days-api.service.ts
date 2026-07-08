import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable, forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export type BusinessDayStatus = 'OPEN' | 'CLOSED';

export interface BusinessDayView {
  id: string;
  date: string;
  status: BusinessDayStatus;
  reason?: string;
  appliesToTenant: boolean;
}

export interface UpsertBusinessDayRequest {
  date: string;
  status: BusinessDayStatus;
  reason?: string;
  appliesToTenant?: boolean;
}

export interface ProviderBusinessDayClosure {
  date: string;
  slotKey: string;
  provider: string;
  reason?: string;
}

interface BusinessDayApiView {
  id: string;
  tenantId: string;
  businessDate: string;
  open: boolean;
  reasonCode?: string | null;
  label?: string | null;
}

interface UpsertBusinessDayApiRequest {
  businessDate: string;
  open: boolean;
  label?: string;
}

interface ResultSlotApiView {
  id: string | { value: string };
  slotKey: string;
  provider: string;
  labelKey?: string | null;
}

interface ResultSlotCalendarOverrideApiView {
  resultSlotId: string | { value: string };
  slotLocalDate?: string | null;
  recurringMd?: string | null;
  available: boolean;
  reasonCode?: string | null;
  reasonLabel?: string | null;
}

@Injectable({ providedIn: 'root' })
export class BusinessDaysApiService {
  private readonly backend = inject(TchBackendClient);

  listBusinessDays(
    params?: { from?: string; to?: string },
    options?: TchRequestOptions,
  ): Observable<BusinessDayView[]> {
    if (!params || (!params.from && !params.to)) {
      return this.backend.get<BusinessDayApiView[]>('/admin/business-days', options).pipe(map(toBusinessDayViews));
    }
    const query = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][],
      ),
    ).toString();
    return this.backend.get<BusinessDayApiView[]>(`/admin/business-days?${query}`, options).pipe(map(toBusinessDayViews));
  }

  upsertBusinessDay(req: UpsertBusinessDayRequest, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/business-days', toApiRequest(req), options);
  }

  listProviderClosures(
    params: { from: string; to: string },
    options?: TchRequestOptions,
  ): Observable<ProviderBusinessDayClosure[]> {
    return this.backend.get<ResultSlotApiView[]>('/platform/result-slots/active', options).pipe(
      switchMap(slots => {
        if (!slots.length) return of([]);
        return forkJoin(
          slots.map(slot => this.backend
            .get<ResultSlotCalendarOverrideApiView[]>(`/platform/result-slots/${resultSlotId(slot.id)}/calendar`, options)
            .pipe(map(overrides => toProviderClosures(slot, overrides, params.from, params.to)))),
        ).pipe(map(groups => groups.flat()));
      }),
    );
  }

  deleteBusinessDay(id: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.delete<void>(`/admin/business-days/${id}`, options);
  }
}

function toApiRequest(req: UpsertBusinessDayRequest): UpsertBusinessDayApiRequest {
  return {
    businessDate: req.date,
    open: req.status === 'OPEN',
    label: req.reason?.trim() || undefined,
  };
}

function toBusinessDayViews(rows: BusinessDayApiView[]): BusinessDayView[] {
  return rows.map(row => ({
    id: row.id,
    date: row.businessDate,
    status: row.open ? 'OPEN' : 'CLOSED',
    reason: row.label ?? row.reasonCode ?? undefined,
    appliesToTenant: true,
  }));
}

function resultSlotId(id: string | { value: string }): string {
  return typeof id === 'string' ? id : id.value;
}

function toProviderClosures(
  slot: ResultSlotApiView,
  overrides: ResultSlotCalendarOverrideApiView[],
  from: string,
  to: string,
): ProviderBusinessDayClosure[] {
  return overrides
    .filter(row => !row.available)
    .flatMap(row => expandProviderClosure(slot, row, from, to));
}

function expandProviderClosure(
  slot: ResultSlotApiView,
  row: ResultSlotCalendarOverrideApiView,
  from: string,
  to: string,
): ProviderBusinessDayClosure[] {
  const reason = row.reasonLabel ?? row.reasonCode ?? slot.labelKey ?? undefined;
  if (row.slotLocalDate) {
    return row.slotLocalDate >= from && row.slotLocalDate <= to
      ? [{ date: row.slotLocalDate, slotKey: slot.slotKey, provider: slot.provider, reason }]
      : [];
  }
  if (!row.recurringMd) return [];

  const startYear = Number(from.slice(0, 4));
  const endYear = Number(to.slice(0, 4));
  const dates: ProviderBusinessDayClosure[] = [];
  for (let year = startYear; year <= endYear; year += 1) {
    const date = `${year}-${row.recurringMd}`;
    if (date >= from && date <= to) {
      dates.push({ date, slotKey: slot.slotKey, provider: slot.provider, reason });
    }
  }
  return dates;
}
