import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { GamesAdminApiService, UpdateGameSettingsRequest } from './games-admin-api.service';
import {
  TenantGamePricingView,
  TenantGameOddGroupView,
  TenantGameOddView,
  TenantGameStatus,
} from './admin-games-pricing.models';
import { consoleBetLabel, consoleGameName, consoleSettlementVariantLabel } from '@tch/web/console';

interface BffPricingEntry {
  betType: string;
  betOption: number | null;
  pricingVariantCode?: string | null;
  odds: number;
}

export interface UpsertTenantOddsRequest {
  readonly gameCode: string;
  readonly pricingVariantCode: string;
  readonly betType: string;
  readonly betOption?: number | null;
  readonly odds: number;
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

  getGamesPricingResource(options?: TchRequestOptions) {
    return this.backend.getResource<TenantGamePricingView[], BffResponse>(
      () => ({ path: '/admin/setup/games-pricing', options }),
      res => res.games.map(row => this.toView(row)),
    );
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

  upsertTenantOdds(req: UpsertTenantOddsRequest, options?: TchRequestOptions): Observable<TenantGameOddView> {
    return this.backend
      .put<BffPricingEntry>('/admin/pricing/odds', req, options)
      .pipe(map(entry => this.toOdd(entry)));
  }

  private toView(row: BffGameRow): TenantGamePricingView {
    const tenantStatus = this.toTenantStatus(row);
    const odds = this.toOdds(row.pricing.entries);
    const oddsGroups = this.toOddsGroups(odds);
    const limits = this.toLimits(row);

    return {
      gameCode:          row.gameCode,
      tenantGameId:      row.tenantGameId?.value ?? null,
      gameName:          consoleGameName(row.gameCode, row.displayName || row.catalogName),
      catalogStatus:     'AVAILABLE',
      tenantStatus,
      pricingProfileLabel: row.pricing.configured ? 'Barème standard' : null,
      odds,
      oddsGroups,
      limits,
      readiness: this.toReadiness(tenantStatus),
    };
  }

  private toTenantStatus(row: BffGameRow): TenantGameStatus {
    if (!row.enabled) return 'INACTIVE';
    if (this.hasStakeSettings(row) && row.pricing.configured) return 'ACTIVE';
    return 'NEEDS_CONFIG';
  }

  private hasStakeSettings(row: BffGameRow): boolean {
    return row.minStake !== null && row.minStake !== undefined &&
      row.maxStake !== null && row.maxStake !== undefined;
  }

  private toOdds(entries: BffPricingEntry[]): TenantGameOddView[] {
    return entries.map(e => this.toOdd(e));
  }

  private toOdd(e: BffPricingEntry): TenantGameOddView {
    return {
      label: this.oddLabel(e),
      value: `×${e.odds}`,
      odds: e.odds,
      betType: e.betType,
      betOption: e.betOption,
      pricingVariantCode: e.pricingVariantCode ?? null,
    };
  }

  private oddLabel(entry: BffPricingEntry): string {
    return consoleBetLabel(entry.betType, entry.betOption);
  }

  private toOddsGroups(odds: readonly TenantGameOddView[]): TenantGameOddGroupView[] {
    const groups = new Map<string, TenantGameOddView[]>();
    for (const odd of odds) {
      const key = `${odd.betType}:${odd.betOption ?? 'none'}`;
      const current = groups.get(key) ?? [];
      current.push(odd);
      groups.set(key, current);
    }

    return Array.from(groups.entries()).map(([id, variants]) => {
      const first = variants[0];
      return {
        id,
        label: first?.label ?? id,
        betType: first?.betType ?? '',
        betOption: first?.betOption ?? null,
        variants: variants.map(variant => ({
          ...variant,
          label: consoleSettlementVariantLabel(variant.pricingVariantCode) ?? variant.label,
        })),
      };
    });
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
