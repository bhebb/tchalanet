import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
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

const BET_OPTION_LABELS: Record<string, Record<number, string>> = {
  MARRIAGE_2D2D: {
    1: 'Ordre exact',
    2: 'Revers / Double',
  },
  LOTTO4_PATTERN: {
    1: 'Exact',
    2: 'Désordre / Box',
    3: '2 premiers chiffres',
    4: '2 derniers chiffres',
  },
  LOTTO5_PATTERN: {
    1: '1er lot + 2e lot',
    2: '1er lot + 3e lot',
    3: 'Mixte 1er/2e/3e lot',
  },
};

interface BffPricingEntry {
  betType: string;
  betOption: number | null;
  odds: number;
}

interface BffLimitAssignment {
  ruleKey: string;
  params: Record<string, unknown> | null;
}

interface BffLimitsView {
  configured: boolean;
  assignments?: BffLimitAssignment[] | null;
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

  getGamesPricing(options?: TchRequestOptions): Observable<TenantGamePricingView[]> {
    return this.backend
      .get<BffResponse>('/admin/setup/games-pricing', options)
      .pipe(map(res => res.games.map(row => this.toView(row))));
  }

  enableGame(gameCode: string, options?: TchRequestOptions): Observable<void> {
    return this.gamesApi.enableGame(gameCode, options);
  }

  disableGame(gameCode: string, options?: TchRequestOptions): Observable<void> {
    return this.gamesApi.disableGame(gameCode, options);
  }

  updateSettings(gameCode: string, req: UpdateGameSettingsRequest, options?: TchRequestOptions): Observable<void> {
    return this.gamesApi.updateGameSettings(gameCode, req, options);
  }

  private toView(row: BffGameRow): TenantGamePricingView {
    const tenantStatus = this.toTenantStatus(row);
    const odds = this.toOdds(row.pricing.entries);
    const limits = this.toLimits(row);

    return {
      gameCode:          row.gameCode,
      tenantGameId:      row.tenantGameId?.value ?? null,
      gameName:          row.displayName || row.catalogName,
      catalogStatus:     'AVAILABLE',
      tenantStatus,
      pricingProfileLabel: row.pricing.configured ? 'Barème standard' : null,
      odds,
      limits,
      readiness: this.toReadiness(tenantStatus),
    };
  }

  private toTenantStatus(row: BffGameRow): TenantGameStatus {
    if (!row.enabled) return 'INACTIVE';
    if (row.limits.configured && row.pricing.configured) return 'ACTIVE';
    return 'NEEDS_CONFIG';
  }

  private toOdds(entries: BffPricingEntry[]): TenantGameOddView[] {
    return entries.map(e => ({
      label: this.oddLabel(e),
      value: `×${e.odds}`,
      betType: e.betType,
      betOption: e.betOption,
    }));
  }

  private oddLabel(entry: BffPricingEntry): string {
    const optionLabel = entry.betOption == null
      ? null
      : (BET_OPTION_LABELS[entry.betType]?.[entry.betOption] ?? `Option ${entry.betOption}`);
    return optionLabel ?? BET_TYPE_LABELS[entry.betType] ?? entry.betType;
  }

  private toLimits(row: BffGameRow): TenantGamePricingView['limits'] {
    return {
      minStake: row.minStake,
      maxStake: row.maxStake ?? this.limitAmount(row.limits, 'MAX_STAKE_PER_LINE'),
      maxPerDraw: this.limitAmount(row.limits, 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW'),
      currency: 'HTG',
    };
  }

  private limitAmount(limits: BffLimitsView, ruleKey: string): number | null {
    const assignment = (limits.assignments ?? []).find(item => item.ruleKey === ruleKey);
    const value = assignment?.params?.['valueCents'];
    if (typeof value === 'number' && Number.isFinite(value)) return value / 100;
    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed / 100 : null;
    }
    return null;
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
