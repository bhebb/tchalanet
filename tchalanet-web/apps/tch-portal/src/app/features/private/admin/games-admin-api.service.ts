import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
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

  updateGameSettings(gameCode: string, req: UpdateGameSettingsRequest): Observable<void> {
    return this.backend.patch<void>(`/admin/games/${gameCode}/settings`, req);
  }
}
