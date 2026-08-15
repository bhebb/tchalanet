import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface TenantGameView {
  readonly gameCode: string;
  readonly catalogName: string;
  readonly category: string | null;
  readonly displayName: string | null;
  readonly enabled: boolean;
  readonly visibleInPos: boolean;
  readonly displayOrder: number;
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly availabilityEnabled: boolean;
  readonly availabilityDays: string | null;
  readonly startLocalTime: string | null;
  readonly endLocalTime: string | null;
  readonly readyForSale: boolean;
  readonly betOptions?: readonly TenantGameBetOptionView[];
  readonly betOptionGroups?: readonly TenantGameBetOptionGroupView[];
}

export interface TenantGameBetOptionView {
  readonly label: string;
  readonly value: string;
  readonly odds: number | null;
  readonly payoutRuleType?: 'STAKE_MULTIPLIER' | 'FIXED_AMOUNT' | null;
  readonly fixedAmount?: number | null;
  readonly betType: string;
  readonly betOption: number | null;
  readonly pricingVariantCode: string | null;
}

export interface TenantGameBetOptionGroupView {
  readonly id: string;
  readonly label: string;
  readonly betType: string;
  readonly betOption: number | null;
  readonly variants: readonly TenantGameBetOptionView[];
}

export type TenantGameSelectionPolicy =
  | 'EXPLICIT_ONLY'
  | 'EXPLICIT_WITH_AUTO_OPTION'
  | 'IMPLICIT_BEST_MATCH';

export interface TenantGameBetOptionConfigView {
  readonly gameCode: string;
  readonly betTypes: readonly TenantBetTypeOptionConfigView[];
}

export interface TenantBetTypeOptionConfigView {
  readonly betType: string;
  readonly selectionPolicy: TenantGameSelectionPolicy;
  readonly defaultOption: number | null;
  readonly options: readonly TenantBetOptionConfigView[];
}

export interface TenantBetOptionConfigView {
  readonly code: number;
  readonly label: string;
  readonly description: string | null;
  readonly enabled: boolean;
  readonly visibleInPos: boolean;
  readonly displayOrder: number;
}

export interface UpdateTenantGameBetOptionConfigRequest {
  readonly betTypes: readonly UpdateTenantBetTypeOptionConfig[];
}

export interface UpdateTenantBetTypeOptionConfig {
  readonly betType: string;
  readonly selectionPolicy: TenantGameSelectionPolicy;
  readonly defaultOption: number | null;
  readonly options: readonly UpdateTenantBetOptionConfig[];
}

export interface UpdateTenantBetOptionConfig {
  readonly code: number;
  readonly enabled: boolean;
  readonly visibleInPos: boolean;
  readonly displayOrder: number;
}

export interface CatalogGameView {
  readonly gameCode: string;
  readonly name: string;
  readonly category: string;
  readonly catalogActive: boolean;
  readonly enabledForTenant: boolean;
  readonly canEnable: boolean;
  readonly disabledReason: string | null;
}

export interface UpdateGameSettingsRequest {
  displayName?: string | null;
  displayOrder?: number | null;
  visibleInPos?: boolean | null;
  minStake?: number | null;
  maxStake?: number | null;
  availabilityEnabled?: boolean | null;
  availabilityDays?: string | null;
  startLocalTime?: string | null;
  endLocalTime?: string | null;
}

@Injectable({ providedIn: 'root' })
export class GamesAdminApiService {
  private readonly backend = inject(TchBackendClient);

  listEnabledGames(options?: TchRequestOptions): Observable<TenantGameView[]> {
    return this.backend.get<TenantGameView[]>('/admin/games', options);
  }

  listEnabledGamesResource(options?: TchRequestOptions) {
    return this.backend.getResource<TenantGameView[]>(() => ({
      path: '/admin/games',
      options,
    }));
  }

  listCatalogGames(options?: TchRequestOptions): Observable<CatalogGameView[]> {
    return this.backend.get<CatalogGameView[]>('/admin/games/catalog', options);
  }

  enableGame(gameCode: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/games/${gameCode}/enable`, {}, options);
  }

  disableGame(gameCode: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/admin/games/${gameCode}/disable`, {}, options);
  }

  updateGameSettings(
    gameCode: string,
    req: UpdateGameSettingsRequest,
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.patch<void>(`/admin/games/${gameCode}/settings`, req, options);
  }

  getBetOptionConfig(
    gameCode: string,
    options?: TchRequestOptions,
  ): Observable<TenantGameBetOptionConfigView> {
    return this.backend.get<TenantGameBetOptionConfigView>(
      `/admin/games/${gameCode}/bet-options`,
      options,
    );
  }

  getBetOptionConfigResource(
    gameCode: () => string | null | undefined,
    options?: TchRequestOptions,
  ) {
    return this.backend.getResource<TenantGameBetOptionConfigView>(() => {
      const code = gameCode();
      return code ? { path: `/admin/games/${code}/bet-options`, options } : undefined;
    });
  }

  updateBetOptionConfig(
    gameCode: string,
    req: UpdateTenantGameBetOptionConfigRequest,
    options?: TchRequestOptions,
  ): Observable<TenantGameBetOptionConfigView> {
    return this.backend.patch<TenantGameBetOptionConfigView>(
      `/admin/games/${gameCode}/bet-options`,
      req,
      options,
    );
  }
}
