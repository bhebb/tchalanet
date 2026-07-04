import { Injectable, ResourceRef, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

// ─── Games ─────────────────────────────────────────────────────────────────
export interface CatalogGameView {
  id: string;
  code: string;
  name: string;
  category: string | null;
  combination: string | null;
  minDigits: number;
  maxDigits: number;
  description: string | null;
  active: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGameRequest {
  code: string;
  name: string;
  category?: string | null;
  combination?: string | null;
  minDigits?: number | null;
  maxDigits?: number | null;
  description?: string | null;
  active?: boolean;
  sortOrder?: number | null;
}

export interface UpdateGameRequest {
  name?: string | null;
  category?: string | null;
  combination?: string | null;
  minDigits?: number | null;
  maxDigits?: number | null;
  description?: string | null;
  active?: boolean | null;
  sortOrder?: number | null;
}

// ─── Result Slots ──────────────────────────────────────────────────────────
export interface CatalogResultSlotView {
  id: string;
  slotKey: string;
  provider: string | null;
  timezone: string | null;
  drawTime: string;
  daysOfWeek: string | null;
  active: boolean;
  labelKey: string | null;
}

export interface CreateResultSlotRequest {
  slotKey: string;
  provider?: string | null;
  timezone?: string | null;
  drawTime: string;
  daysOfWeek?: string | null;
  labelKey?: string | null;
}

export interface UpdateResultSlotRequest {
  provider?: string | null;
  timezone?: string | null;
  drawTime?: string | null;
  daysOfWeek?: string | null;
  labelKey?: string | null;
  active?: boolean | null;
}

// ─── Plans ─────────────────────────────────────────────────────────────────
export interface CatalogPlanView {
  id: string;
  code: string;
  name: string;
  description: string | null;
  priceAmount: number | null;
  currency: string | null;
  billingPeriod: string | null;
  limitsJson: unknown;
  featuresJson: unknown;
  active: boolean;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  code: string;
  name: string;
  description?: string | null;
  priceAmount?: number | null;
  currency?: string | null;
  billingPeriod?: string | null;
  active?: boolean;
}

export interface UpdatePlanRequest {
  name?: string | null;
  description?: string | null;
  priceAmount?: number | null;
  currency?: string | null;
  billingPeriod?: string | null;
  active?: boolean | null;
}

// ─── Themes ────────────────────────────────────────────────────────────────
export interface CatalogThemeView {
  id: string;
  code: string;
  vendor: string | null;
  config: unknown;
  labelKey: string | null;
  active: boolean;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateThemeRequest {
  code: string;
  vendor?: string | null;
  config?: string | null;
  labelKey?: string | null;
  active?: boolean;
}

export interface UpdateThemeRequest {
  vendor?: string | null;
  config?: string | null;
  labelKey?: string | null;
  active?: boolean | null;
}

// ─── Pricing ───────────────────────────────────────────────────────────────
export type BetType =
  | 'MATCH_1_2D'
  | 'MATCH_2_2D'
  | 'MATCH_3_2D'
  | 'LOTTO3_3D'
  | 'MARRIAGE_2D2D'
  | 'LOTTO4_PATTERN'
  | 'LOTTO5_PATTERN';

export interface CatalogPricingView {
  id: string;
  tenantId: string | null;
  gameCode: string;
  betType: BetType;
  betOption: number | null;
  odds: number;
  active: boolean;
}

export interface CreatePricingRequest {
  tenantId?: string | null;
  gameCode: string;
  betType: BetType;
  betOption?: number | null;
  odds: number;
  active?: boolean;
}

export interface UpdatePricingRequest {
  tenantId?: string | null;
  gameCode?: string | null;
  betType?: BetType | null;
  betOption?: number | null;
  odds?: number | null;
  active?: boolean | null;
}

// ─── Draw Channels ─────────────────────────────────────────────────────────
export interface CatalogDrawChannelView {
  id: string;
  tenantId: string | null;
  code: string;
  name: string;
  label: string | null;
  timezone: string | null;
  drawTime: string;
  cutoffSec: number | null;
  daysOfWeek: string[] | null;
  active: boolean;
  sortOrder: number;
  period: string | null;
  notes: string | null;
  resultSlotId: string | null;
  defaultSource: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDrawChannelRequest {
  code: string;
  name: string;
  label?: string | null;
  timezone?: string | null;
  drawTime: string;
  cutoffSec?: number | null;
  daysOfWeek?: string[] | null;
  active?: boolean;
  sortOrder?: number | null;
  period?: string | null;
  notes?: string | null;
  resultSlotId?: string | null;
  defaultSource?: string | null;
}

export interface UpdateDrawChannelRequest {
  code?: string | null;
  name?: string | null;
  label?: string | null;
  timezone?: string | null;
  drawTime?: string | null;
  cutoffSec?: number | null;
  daysOfWeek?: string[] | null;
  active?: boolean | null;
  sortOrder?: number | null;
  period?: string | null;
  notes?: string | null;
  resultSlotId?: string | null;
  defaultSource?: string | null;
}

export type CatalogId = string | { value: string };

export interface CatalogDrawChannelGameView {
  id: CatalogId;
  drawChannelId: CatalogId;
  tenantGameId: CatalogId;
  enabled: boolean;
  flags: unknown;
}

export interface CreateDrawChannelGameRequest {
  tenantGameId: string;
  enabled: boolean;
  flags?: unknown;
}

export interface UpdateDrawChannelGameRequest {
  enabled?: boolean | null;
  flags?: unknown;
}

export interface CatalogResultSlotCalendarOverrideView {
  id: CatalogId;
  resultSlotId: CatalogId;
  slotLocalDate: string | null;
  recurringMd: string | null;
  available: boolean;
  reasonCode: string;
  reasonLabel: string | null;
}

export interface CreateResultSlotCalendarOverrideRequest {
  slotLocalDate?: string | null;
  recurringMd?: string | null;
  available: boolean;
  reasonCode: string;
  reasonLabel?: string | null;
}

export interface UpdateResultSlotCalendarOverrideRequest {
  available: boolean;
  reasonCode: string;
  reasonLabel?: string | null;
}

export interface CatalogGameListParams {
  readonly active?: boolean;
  readonly q?: string;
  readonly page?: number;
  readonly size?: number;
}

export interface CatalogDrawChannelListParams {
  readonly q?: string;
  readonly active?: boolean;
  readonly page?: number;
  readonly size?: number;
}

// ─── Service ───────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class PlatformCatalogApi {
  private readonly backend = inject(TchBackendClient);

  // Games
  listGames(
    params: CatalogGameListParams = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<CatalogGameView>> {
    return this.backend.getPage<CatalogGameView>('/platform/catalog/games', {
      ...options,
      params: catalogGameListQueryParams(params),
    });
  }

  listGamesResource(
    params: () => CatalogGameListParams,
    options?: TchRequestOptions,
  ): ResourceRef<TchPage<CatalogGameView> | undefined> {
    return this.backend.getPageResource<CatalogGameView>(() => ({
      path: '/platform/catalog/games',
      options: {
        ...options,
        params: catalogGameListQueryParams(params()),
      },
    }));
  }

  createGame(req: CreateGameRequest): Observable<CatalogGameView> {
    return this.backend.post<CatalogGameView>('/platform/catalog/games', req);
  }

  updateGame(id: string, req: UpdateGameRequest): Observable<CatalogGameView> {
    return this.backend.put<CatalogGameView>(`/platform/catalog/games/${id}`, req);
  }

  deactivateGame(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/catalog/games/${id}/deactivate`, {});
  }

  deleteGame(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/catalog/games/${id}`);
  }

  // Result Slots
  listResultSlots(): Observable<CatalogResultSlotView[]> {
    return this.backend.get<CatalogResultSlotView[]>('/platform/result-slots/active');
  }

  listResultSlotsResource(
    options?: TchRequestOptions,
  ): ResourceRef<CatalogResultSlotView[] | undefined> {
    return this.backend.getResource<CatalogResultSlotView[]>(() => ({
      path: '/platform/result-slots/active',
      options,
    }));
  }

  createResultSlot(req: CreateResultSlotRequest): Observable<CatalogResultSlotView> {
    return this.backend.post<CatalogResultSlotView>('/platform/result-slots', req);
  }

  updateResultSlot(id: string, req: UpdateResultSlotRequest): Observable<CatalogResultSlotView> {
    return this.backend.put<CatalogResultSlotView>(`/platform/result-slots/${id}`, req);
  }

  disableResultSlot(slotKey: string): Observable<void> {
    return this.backend.post<void>(`/platform/result-slots/${slotKey}/disable`, {});
  }

  deleteResultSlot(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/result-slots/${id}`);
  }

  // Plans
  listPlans(): Observable<CatalogPlanView[]> {
    return this.backend.get<CatalogPlanView[]>('/platform/plans');
  }

  listPlansResource(options?: TchRequestOptions): ResourceRef<CatalogPlanView[] | undefined> {
    return this.backend.getResource<CatalogPlanView[]>(() => ({
      path: '/platform/plans',
      options,
    }));
  }

  createPlan(req: CreatePlanRequest): Observable<CatalogPlanView> {
    return this.backend.post<CatalogPlanView>('/platform/plans', req);
  }

  updatePlan(id: string, req: UpdatePlanRequest): Observable<CatalogPlanView> {
    return this.backend.put<CatalogPlanView>(`/platform/plans/${id}`, req);
  }

  deactivatePlan(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/plans/${id}/deactivate`, {});
  }

  deletePlan(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/plans/${id}`);
  }

  // Themes
  listThemes(): Observable<CatalogThemeView[]> {
    return this.backend.get<CatalogThemeView[]>('/platform/catalog/theme-presets');
  }

  listThemesResource(options?: TchRequestOptions): ResourceRef<CatalogThemeView[] | undefined> {
    return this.backend.getResource<CatalogThemeView[]>(() => ({
      path: '/platform/catalog/theme-presets',
      options,
    }));
  }

  createTheme(req: CreateThemeRequest): Observable<CatalogThemeView> {
    return this.backend.post<CatalogThemeView>('/platform/catalog/theme-presets', req);
  }

  updateTheme(id: string, req: UpdateThemeRequest): Observable<CatalogThemeView> {
    return this.backend.put<CatalogThemeView>(`/platform/catalog/theme-presets/${id}`, req);
  }

  deleteTheme(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/catalog/theme-presets/${id}`);
  }

  // Draw Channels
  listDrawChannels(
    params: CatalogDrawChannelListParams = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<CatalogDrawChannelView>> {
    return this.backend.getPage<CatalogDrawChannelView>('/platform/draw-channels', {
      ...options,
      params: catalogDrawChannelListQueryParams(params),
    });
  }

  listDrawChannelsResource(
    params: () => CatalogDrawChannelListParams,
    options?: TchRequestOptions,
  ): ResourceRef<TchPage<CatalogDrawChannelView> | undefined> {
    return this.backend.getPageResource<CatalogDrawChannelView>(() => ({
      path: '/platform/draw-channels',
      options: {
        ...options,
        params: catalogDrawChannelListQueryParams(params()),
      },
    }));
  }

  createDrawChannel(req: CreateDrawChannelRequest): Observable<CatalogDrawChannelView> {
    return this.backend.post<CatalogDrawChannelView>('/platform/draw-channels', req);
  }

  updateDrawChannel(id: string, req: UpdateDrawChannelRequest): Observable<CatalogDrawChannelView> {
    return this.backend.put<CatalogDrawChannelView>(`/platform/draw-channels/${id}`, req);
  }

  disableDrawChannel(id: string): Observable<void> {
    return this.backend.post<void>(`/platform/draw-channels/${id}/disable`, {});
  }

  deleteDrawChannel(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/draw-channels/${id}`);
  }

  listDrawChannelGames(channelId: string): Observable<CatalogDrawChannelGameView[]> {
    return this.backend.get<CatalogDrawChannelGameView[]>(`/platform/draw-channels/${channelId}/games`);
  }

  listDrawChannelGamesResource(
    channelId: () => string | undefined,
    options?: TchRequestOptions,
  ): ResourceRef<CatalogDrawChannelGameView[] | undefined> {
    return this.backend.getResource<CatalogDrawChannelGameView[]>(() => {
      const id = channelId();
      return id
        ? {
            path: `/platform/draw-channels/${id}/games`,
            options,
          }
        : undefined;
    });
  }

  upsertDrawChannelGame(
    channelId: string,
    req: CreateDrawChannelGameRequest,
  ): Observable<CatalogDrawChannelGameView> {
    return this.backend.post<CatalogDrawChannelGameView>(
      `/platform/draw-channels/${channelId}/games`,
      req,
    );
  }

  updateDrawChannelGame(
    channelId: string,
    tenantGameId: string,
    req: UpdateDrawChannelGameRequest,
  ): Observable<CatalogDrawChannelGameView> {
    return this.backend.put<CatalogDrawChannelGameView>(
      `/platform/draw-channels/${channelId}/games/${tenantGameId}`,
      req,
    );
  }

  deleteDrawChannelGame(channelId: string, tenantGameId: string): Observable<void> {
    return this.backend.delete<void>(`/platform/draw-channels/${channelId}/games/${tenantGameId}`);
  }

  listResultSlotCalendarOverrides(slotId: string): Observable<CatalogResultSlotCalendarOverrideView[]> {
    return this.backend.get<CatalogResultSlotCalendarOverrideView[]>(
      `/platform/result-slots/${slotId}/calendar`,
    );
  }

  listResultSlotCalendarOverridesResource(
    slotId: () => string | undefined,
    options?: TchRequestOptions,
  ): ResourceRef<CatalogResultSlotCalendarOverrideView[] | undefined> {
    return this.backend.getResource<CatalogResultSlotCalendarOverrideView[]>(() => {
      const id = slotId();
      return id
        ? {
            path: `/platform/result-slots/${id}/calendar`,
            options,
          }
        : undefined;
    });
  }

  createResultSlotCalendarOverride(
    slotId: string,
    req: CreateResultSlotCalendarOverrideRequest,
  ): Observable<CatalogResultSlotCalendarOverrideView> {
    return this.backend.post<CatalogResultSlotCalendarOverrideView>(
      `/platform/result-slots/${slotId}/calendar`,
      req,
    );
  }

  updateResultSlotCalendarOverride(
    slotId: string,
    overrideId: string,
    req: UpdateResultSlotCalendarOverrideRequest,
  ): Observable<CatalogResultSlotCalendarOverrideView> {
    return this.backend.put<CatalogResultSlotCalendarOverrideView>(
      `/platform/result-slots/${slotId}/calendar/${overrideId}`,
      req,
    );
  }

  deleteResultSlotCalendarOverride(slotId: string, overrideId: string): Observable<void> {
    return this.backend.delete<void>(`/platform/result-slots/${slotId}/calendar/${overrideId}`);
  }

  // Pricing
  listPricing(): Observable<CatalogPricingView[]> {
    return this.backend.get<CatalogPricingView[]>('/platform/pricing');
  }

  listPricingResource(options?: TchRequestOptions): ResourceRef<CatalogPricingView[] | undefined> {
    return this.backend.getResource<CatalogPricingView[]>(() => ({
      path: '/platform/pricing',
      options,
    }));
  }

  createPricing(req: CreatePricingRequest): Observable<CatalogPricingView> {
    return this.backend.post<CatalogPricingView>('/platform/pricing', req);
  }

  updatePricing(id: string, req: UpdatePricingRequest): Observable<CatalogPricingView> {
    return this.backend.put<CatalogPricingView>(`/platform/pricing/${id}`, req);
  }

  deletePricing(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/pricing/${id}`);
  }
}

function catalogGameListQueryParams(params: CatalogGameListParams): Record<string, string> {
  return {
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
    ...(params.active != null ? { active: String(params.active) } : {}),
    ...(params.q ? { q: params.q } : {}),
  };
}

function catalogDrawChannelListQueryParams(params: CatalogDrawChannelListParams): Record<string, string> {
  return {
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
    ...(params.q ? { nameContains: params.q } : {}),
    ...(params.active != null ? { active: String(params.active) } : {}),
  };
}
