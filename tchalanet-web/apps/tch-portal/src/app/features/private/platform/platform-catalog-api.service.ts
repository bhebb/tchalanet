import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

// ─── Shared ────────────────────────────────────────────────────────────────
export interface TchPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ─── Games ─────────────────────────────────────────────────────────────────
export interface CatalogGameView {
  id: { value: string };
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
  id: { value: string };
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

// ─── Plans ─────────────────────────────────────────────────────────────────
export interface CatalogPlanView {
  id: { value: string };
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

// ─── Themes ────────────────────────────────────────────────────────────────
export interface CatalogThemeView {
  id: { value: string };
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

// ─── Pricing ───────────────────────────────────────────────────────────────
export type BetType =
  | 'MATCH_1_2D' | 'MATCH_2_2D' | 'MATCH_3_2D'
  | 'LOTTO3_3D' | 'MARRIAGE_2D2D'
  | 'LOTTO4_PATTERN' | 'LOTTO5_PATTERN';

export interface CatalogPricingView {
  id: { value: string };
  tenantId: { value: string } | null;
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

// ─── Service ───────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class PlatformCatalogApi {
  private readonly backend = inject(TchBackendClient);

  // Games
  listGames(params: { active?: boolean; q?: string; page?: number; size?: number } = {}): Observable<TchPage<CatalogGameView>> {
    const q = new URLSearchParams();
    if (params.active != null) q.set('active', String(params.active));
    if (params.q) q.set('q', params.q);
    if (params.page != null) q.set('page', String(params.page));
    if (params.size != null) q.set('size', String(params.size));
    const qs = q.toString();
    return this.backend.get<TchPage<CatalogGameView>>(`/platform/catalog/games${qs ? '?' + qs : ''}`);
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

  createResultSlot(req: CreateResultSlotRequest): Observable<CatalogResultSlotView> {
    return this.backend.post<CatalogResultSlotView>('/platform/result-slots', req);
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

  createPlan(req: CreatePlanRequest): Observable<CatalogPlanView> {
    return this.backend.post<CatalogPlanView>('/platform/plans', req);
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

  createTheme(req: CreateThemeRequest): Observable<CatalogThemeView> {
    return this.backend.post<CatalogThemeView>('/platform/catalog/theme-presets', req);
  }

  deleteTheme(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/catalog/theme-presets/${id}`);
  }

  // Pricing
  listPricing(): Observable<CatalogPricingView[]> {
    return this.backend.get<CatalogPricingView[]>('/platform/pricing');
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
