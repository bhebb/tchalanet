import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface GameSettings {
  visibleInPos?: boolean;
  minStake?: number;
  maxStake?: number;
  enabledDrawSlots?: string[];
  displayOrder?: number;
}

export interface TenantGameView {
  gameCode: string;
  displayName: string;
  enabled: boolean;
  settings: GameSettings;
}

export interface CatalogGameView {
  gameCode: string;
  displayName: string;
  category: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class GamesAdminApiService {
  private readonly backend = inject(TchBackendClient);

  listEnabledGames(): Observable<TenantGameView[]> {
    return this.backend.get<TenantGameView[]>('/admin/games');
  }

  listCatalogGames(): Observable<CatalogGameView[]> {
    return this.backend.get<CatalogGameView[]>('/admin/games/catalog');
  }

  enableGame(gameCode: string): Observable<void> {
    return this.backend.post<void>(`/admin/games/${gameCode}/enable`, {});
  }

  disableGame(gameCode: string): Observable<void> {
    return this.backend.post<void>(`/admin/games/${gameCode}/disable`, {});
  }

  updateGameSettings(gameCode: string, settings: GameSettings): Observable<void> {
    return this.backend.patch<void>(`/admin/games/${gameCode}/settings`, settings);
  }
}
