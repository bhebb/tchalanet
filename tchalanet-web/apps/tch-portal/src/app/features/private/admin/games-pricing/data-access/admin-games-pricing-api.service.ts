import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { TchBackendClient } from '@tch/api';
import { GamesAdminApiService, UpdateGameSettingsRequest } from '../../games-admin-api.service';
import {
  TenantGamePricingView,
  TenantGameOddView,
  TenantGameStatus,
} from './admin-games-pricing.models';

const BET_TYPE_LABELS: Record<string, string> = {
  STRAIGHT:   'Straight',
  BOX:        'Box',
  FRONT_PAIR: 'Front Pair',
  BACK_PAIR:  'Back Pair',
  LOTTO:      'Loto',
  COMBO:      'Combo',
  MARIAGE:    'Mariage',
};

interface BffPricingEntry {
  betType: string;
  betOption: number | null;
  odds: number;
}

interface BffLimitsView {
  configured: boolean;
  assignments: unknown[];
}

interface BffPricingView {
  configured: boolean;
  entries: BffPricingEntry[];
}

interface BffGameRow {
  gameCode: string;
  tenantGameId: { value: string } | null;
  catalogName: string;
  displayName: string;
  enabled: boolean;
  visibleInPos: boolean;
  minStake: number | null;
  maxStake: number | null;
  limits: BffLimitsView;
  pricing: BffPricingView;
}

interface BffResponse {
  games: BffGameRow[];
}

@Injectable({ providedIn: 'root' })
export class AdminGamesPricingApiService {
  private readonly backend = inject(TchBackendClient);
  private readonly gamesApi = inject(GamesAdminApiService);

  getGamesPricing(): Observable<TenantGamePricingView[]> {
    return this.backend
      .get<BffResponse>('/admin/setup/games-pricing')
      .pipe(map(res => res.games.map(row => this.toView(row))));
  }

  enableGame(gameCode: string): Observable<void> {
    return this.gamesApi.enableGame(gameCode);
  }

  disableGame(gameCode: string): Observable<void> {
    return this.gamesApi.disableGame(gameCode);
  }

  updateSettings(gameCode: string, req: UpdateGameSettingsRequest): Observable<void> {
    return this.gamesApi.updateGameSettings(gameCode, req);
  }

  private toView(row: BffGameRow): TenantGamePricingView {
    const tenantStatus = this.toTenantStatus(row);
    const odds = this.toOdds(row.pricing.entries);

    return {
      gameCode:          row.gameCode,
      tenantGameId:      row.tenantGameId?.value ?? null,
      gameName:          row.displayName || row.catalogName,
      catalogStatus:     'AVAILABLE',
      tenantStatus,
      pricingProfileLabel: row.pricing.configured ? 'Barème standard' : null,
      odds,
      limits: {
        minStake:   row.minStake,
        maxStake:   row.maxStake,
        maxPerDraw: null,
        currency:   'HTG',
      },
      readiness: this.toReadiness(tenantStatus),
    };
  }

  private toTenantStatus(row: BffGameRow): TenantGameStatus {
    if (!row.enabled) return 'INACTIVE';
    if (row.limits.configured && row.pricing.configured) return 'ACTIVE';
    return 'NEEDS_CONFIG';
  }

  private toOdds(entries: BffPricingEntry[]): TenantGameOddView[] {
    return entries.slice(0, 4).map(e => ({
      label: BET_TYPE_LABELS[e.betType] ?? e.betType,
      value: `×${e.odds}`,
    }));
  }

  private toReadiness(status: TenantGameStatus): TenantGamePricingView['readiness'] {
    switch (status) {
      case 'ACTIVE':       return { status: 'READY',   label: 'Prêt',           reason: null };
      case 'NEEDS_CONFIG': return { status: 'TODO',    label: 'À configurer',   reason: 'Limites ou barème manquant' };
      case 'INACTIVE':     return { status: 'TODO',    label: 'Inactif',        reason: null };
      case 'UNAVAILABLE':  return { status: 'BLOCKED', label: 'Non disponible', reason: null };
    }
  }
}
