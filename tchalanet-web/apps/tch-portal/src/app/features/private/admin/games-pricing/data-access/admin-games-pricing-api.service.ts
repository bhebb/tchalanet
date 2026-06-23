import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import { GamesAdminApiService, TenantGameView, CatalogGameView, UpdateGameSettingsRequest } from '../../games-admin-api.service';
import { BaremesAdminApi, PricingOddsEntry } from '../../baremes-admin.api.service';
import {
  TenantGamePricingView,
  TenantGameOddView,
  TenantGameLimitView,
  CatalogStatus,
  TenantGameStatus,
} from './admin-games-pricing.models';

const BET_TYPE_LABELS: Record<string, string> = {
  STRAIGHT: 'Straight',
  BOX: 'Box',
  FRONT_PAIR: 'Front Pair',
  BACK_PAIR: 'Back Pair',
  LOTTO: 'Loto',
  COMBO: 'Combo',
  MARIAGE: 'Mariage',
};

@Injectable({ providedIn: 'root' })
export class AdminGamesPricingApiService {
  private readonly gamesApi = inject(GamesAdminApiService);
  private readonly baremesApi = inject(BaremesAdminApi);

  getGamesPricing(): Observable<TenantGamePricingView[]> {
    return forkJoin({
      catalog: this.gamesApi.listCatalogGames(),
      enabled: this.gamesApi.listEnabledGames(),
      odds: this.baremesApi.listTenantOdds(),
    }).pipe(
      map(({ catalog, enabled, odds }) =>
        this.assemble(catalog, enabled, odds),
      ),
    );
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

  private assemble(
    catalog: CatalogGameView[],
    enabled: TenantGameView[],
    odds: PricingOddsEntry[],
  ): TenantGamePricingView[] {
    const enabledMap = new Map<string, TenantGameView>(
      enabled.map(g => [g.gameCode, g]),
    );
    const oddsMap = new Map<string, PricingOddsEntry[]>();
    for (const entry of odds) {
      if (!oddsMap.has(entry.gameCode)) oddsMap.set(entry.gameCode, []);
      oddsMap.get(entry.gameCode)!.push(entry);
    }

    return catalog.map(item => {
      const tenantGame = enabledMap.get(item.gameCode) ?? null;
      const catalogStatus = this.toCatalogStatus(item);
      const tenantStatus = this.toTenantStatus(item, tenantGame);
      const gameOdds = this.toOdds(oddsMap.get(item.gameCode) ?? []);
      const limits = this.toLimits(tenantGame);
      const readiness = this.toReadiness(tenantStatus);

      return {
        gameCode: item.gameCode,
        gameName: tenantGame?.displayName ?? item.name,
        catalogStatus,
        tenantStatus,
        pricingProfileLabel: gameOdds.length > 0 ? 'Barème standard' : null,
        odds: gameOdds,
        limits,
        readiness,
      } satisfies TenantGamePricingView;
    });
  }

  private toCatalogStatus(item: CatalogGameView): CatalogStatus {
    if (!item.catalogActive) return 'DISABLED';
    if (!item.canEnable && !item.enabledForTenant) return 'COMING_SOON';
    return 'AVAILABLE';
  }

  private toTenantStatus(item: CatalogGameView, tenantGame: TenantGameView | null): TenantGameStatus {
    if (!item.catalogActive && !item.enabledForTenant) return 'UNAVAILABLE';
    if (!item.enabledForTenant) return 'INACTIVE';
    if (tenantGame?.readyForSale) return 'ACTIVE';
    return 'NEEDS_CONFIG';
  }

  private toOdds(entries: PricingOddsEntry[]): TenantGameOddView[] {
    return entries
      .filter(e => e.active)
      .slice(0, 4)
      .map(e => ({
        label: BET_TYPE_LABELS[e.betType] ?? e.betType,
        value: `×${e.odds}`,
      }));
  }

  private toLimits(tenantGame: TenantGameView | null): TenantGameLimitView {
    return {
      minStake: tenantGame?.minStake ?? null,
      maxStake: tenantGame?.maxStake ?? null,
      maxPerDraw: null,
      currency: 'HTG',
    };
  }

  private toReadiness(status: TenantGameStatus): TenantGamePricingView['readiness'] {
    switch (status) {
      case 'ACTIVE':      return { status: 'READY',   label: 'Prêt',           reason: null };
      case 'NEEDS_CONFIG': return { status: 'TODO',    label: 'À configurer',   reason: 'Limites ou barème manquant' };
      case 'INACTIVE':    return { status: 'TODO',     label: 'Inactif',        reason: null };
      case 'UNAVAILABLE': return { status: 'BLOCKED',  label: 'Non disponible', reason: null };
    }
  }
}
