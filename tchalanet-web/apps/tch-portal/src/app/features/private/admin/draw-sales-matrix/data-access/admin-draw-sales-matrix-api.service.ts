import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export interface MatrixSummary {
  providerCount: number;
  slotCount: number;
  configuredChannelCount: number;
  activeChannelCount: number;
  supportedTenantGameCount: number;
  offeredChannelGameCount: number;
  saleReadyChannelGameCount: number;
  missingStakeConfigCount: number;
  missingLimitCount: number;
}

export interface SetupWarning {
  code: string;
  severity: 'ERROR' | 'WARN' | 'INFO';
}

export interface LimitAssignmentRow {
  ruleKey: { value: string };
  enabled: boolean;
  onBreach: string;
  params: unknown;
}

export interface LimitsSetupView {
  configured: boolean;
  assignments: LimitAssignmentRow[];
}

export interface ChannelGameSetupView {
  gameCode: string;
  tenantGameId: { value: string };
  displayName: string | null;
  enabledForTenant: boolean;
  visibleInPos: boolean;
  offeredOnChannel: boolean;
  enabledOnChannel: boolean;
  minStake: number | null;
  maxStake: number | null;
  limits: LimitsSetupView;
  saleReady: boolean;
  warnings: SetupWarning[];
}

export interface DrawChannelSetupView {
  drawChannelId: { value: string };
  channelCode: string;
  active: boolean;
  configured: boolean;
  drawTime: string | null;
  salesOpenTime: string | null;
  cutoffSec: number;
  defaultSource: string | null;
  sortOrder: number;
  dependsOnChannelId: { value: string } | null;
}

export interface ResultSlotSetupView {
  resultSlotId: { value: string };
  drawTime: string | null;
  daysOfWeek: string | null;
  active: boolean;
}

export interface SlotMatrixView {
  slotKey: string;
  labelKey: string | null;
  resultSlot: ResultSlotSetupView;
  channel: DrawChannelSetupView | null;
  games: ChannelGameSetupView[];
  slotReady: boolean;
  warnings: SetupWarning[];
}

export interface ProviderMatrixView {
  providerCode: string;
  slots: SlotMatrixView[];
}

export interface TenantDrawSalesMatrixView {
  summary: MatrixSummary;
  providers: ProviderMatrixView[];
}

@Injectable({ providedIn: 'root' })
export class AdminDrawSalesMatrixApi {
  private readonly backend = inject(TchBackendClient);

  getMatrix(): Observable<TenantDrawSalesMatrixView> {
    return this.backend.get<TenantDrawSalesMatrixView>('/admin/setup/draw-sales-matrix');
  }

  offerGame(drawChannelId: string, tenantGameId: string): Observable<unknown> {
    return this.backend.put<unknown>(
      `/admin/draw-channels/${drawChannelId}/tenant-games/${tenantGameId}`,
      { enabled: true },
    );
  }

  toggleGame(drawChannelId: string, tenantGameId: string, enabled: boolean): Observable<unknown> {
    return this.backend.patch<unknown>(
      `/admin/draw-channels/${drawChannelId}/tenant-games/${tenantGameId}`,
      { enabled },
    );
  }

  removeGame(drawChannelId: string, tenantGameId: string): Observable<unknown> {
    return this.backend.delete<unknown>(
      `/admin/draw-channels/${drawChannelId}/tenant-games/${tenantGameId}`,
    );
  }
}
