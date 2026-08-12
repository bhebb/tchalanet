import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { consoleDrawIdentity } from '@tch/web/console';
import type { ConsoleDrawResultSourceKind } from '@tch/web/console';
import {
  DrawChannelProviderView,
  DrawChannelSlotConfigView,
} from './admin-draw-channels.models';
import type {
  ProviderMatrixView,
  SlotMatrixView,
  TenantDrawSalesMatrixView,
} from '../../draw-sales-matrix/data-access/admin-draw-sales-matrix-api.service';

@Injectable({ providedIn: 'root' })
export class AdminDrawChannelsApiService {
  private readonly backend = inject(TchBackendClient);

  getDrawChannelProviders(options?: TchRequestOptions): Observable<DrawChannelProviderView[]> {
    return this.backend
      .get<TenantDrawSalesMatrixView>('/admin/setup/draw-sales-matrix', options)
      .pipe(map(matrix => matrix.providers.map(provider => this.toProvider(provider))));
  }

  private toProvider(provider: ProviderMatrixView): DrawChannelProviderView {
    const slots = provider.slots.map(slot => this.toSlot(provider.providerCode, slot));
    const configuredChannelCount = provider.slots.filter(slot => slot.channel !== null).length;
    const activeChannelCount = provider.slots.filter(slot => slot.channel?.active === true).length;
    const offeredGameCount = provider.slots.reduce(
      (sum, slot) => sum + slot.games.filter(game => game.offeredOnChannel).length,
      0,
    );
    const saleReadyGameCount = provider.slots.reduce(
      (sum, slot) => sum + slot.games.filter(game => game.saleReady).length,
      0,
    );
    const tenantStatus = this.providerStatus(activeChannelCount, saleReadyGameCount, configuredChannelCount);
    const identity = consoleDrawIdentity({ providerCode: provider.providerCode });
    const resultAcquisition = this.resultAcquisition(provider.slots, activeChannelCount);

    return {
      providerCode: provider.providerCode,
      providerLabel: identity.providerName ?? provider.providerCode,
      timezone: provider.providerCode,
      profileLabelKey: 'admin.drawChannels.profile.haitiLottery',
      tenantStatus,
      resultAcquisition,
      defaultSalesCutoffMinutes: this.defaultSalesCutoffMinutes(provider.slots),
      configuredChannelCount,
      activeChannelCount,
      offeredGameCount,
      saleReadyGameCount,
      slots,
      readiness: {
        status: tenantStatus === 'ACTIVE' ? 'READY' : 'TODO',
        label:
          tenantStatus === 'ACTIVE'
            ? 'admin.drawChannels.readiness.ready'
            : 'admin.drawChannels.readiness.todo',
        reason: this.readinessReason(tenantStatus, configuredChannelCount, offeredGameCount),
      },
    };
  }

  private toSlot(providerCode: string, slot: SlotMatrixView): DrawChannelSlotConfigView {
    const identity = consoleDrawIdentity({
      providerCode,
      slotKey: slot.slotKey,
      channelCode: slot.channel?.channelCode ?? null,
      localTimeLabel: slot.channel?.drawTime ?? null,
      officialTimeLabel: slot.resultSlot.drawTime,
    });
    const offeredGameCount = slot.games.filter(game => game.offeredOnChannel).length;

    return {
      channelId: slot.channel?.drawChannelId.value ?? null,
      slotKey: slot.slotKey,
      label: identity.channelName ?? identity.slotLabel ?? slot.slotKey,
      enabled: slot.channel?.active === true,
      drawTime: slot.channel?.drawTime ?? slot.resultSlot.drawTime,
      salesCutoffMinutes:
        slot.channel?.cutoffSec === undefined || slot.channel?.cutoffSec === null
          ? null
          : Math.round(slot.channel.cutoffSec / 60),
      offeredGameCount,
      saleReadyGameCount: slot.games.filter(game => game.saleReady).length,
    };
  }

  private providerStatus(
    activeChannelCount: number,
    saleReadyGameCount: number,
    configuredChannelCount: number,
  ): DrawChannelProviderView['tenantStatus'] {
    if (activeChannelCount === 0) return configuredChannelCount > 0 ? 'INACTIVE' : 'NEEDS_CONFIG';
    return saleReadyGameCount > 0 ? 'ACTIVE' : 'NEEDS_CONFIG';
  }

  private defaultSalesCutoffMinutes(slots: readonly SlotMatrixView[]): number | null {
    const firstConfigured = slots.find(slot => slot.channel?.cutoffSec !== undefined);
    return firstConfigured?.channel ? Math.round(firstConfigured.channel.cutoffSec / 60) : null;
  }

  private resultAcquisition(
    slots: readonly SlotMatrixView[],
    activeChannelCount: number,
  ): DrawChannelProviderView['resultAcquisition'] {
    const defaultSource = slots.map(slot => slot.channel?.defaultSource).find(Boolean);
    const source = this.resultSourceKind(defaultSource);

    if (source !== null && source !== 'MANUAL') {
      return {
        mode: 'AUTO',
        source,
        sourceStatus: 'OK',
      };
    }

    if (activeChannelCount > 0) {
      return {
        mode: 'MANUAL',
        source: 'MANUAL',
        sourceStatus: 'OK',
      };
    }

    return {
      mode: 'UNCONFIGURED',
      sourceStatus: 'UNCONFIGURED',
    };
  }

  private resultSourceKind(source: string | null | undefined): ConsoleDrawResultSourceKind | null {
    if (!source) return null;
    const normalized = source.toUpperCase();
    if (normalized.includes('RSS')) return 'RSS';
    if (normalized.includes('BATCH')) return 'BATCH';
    if (normalized.includes('MANUAL')) return 'MANUAL';
    if (normalized.includes('API') || normalized.includes('HTTP')) return 'API';
    return 'API';
  }

  private readinessReason(
    status: DrawChannelProviderView['tenantStatus'],
    configuredChannelCount: number,
    offeredGameCount: number,
  ): string | null {
    if (status === 'ACTIVE') return null;
    if (configuredChannelCount === 0) return 'admin.drawChannels.readiness.reason.noChannels';
    if (offeredGameCount === 0) return 'admin.drawChannels.readiness.reason.noGames';
    return 'admin.drawChannels.readiness.reason.reviewCoverage';
  }
}
