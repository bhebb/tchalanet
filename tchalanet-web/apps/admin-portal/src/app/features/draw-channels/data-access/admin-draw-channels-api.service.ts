import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { consoleDrawIdentity } from '@tch/web/console';
import {
  DrawChannelDetailView,
  DrawChannelProviderView,
  DrawChannelSlotConfigView,
  UpdateTenantDrawChannelRequest,
} from './admin-draw-channels.models';

interface TenantDrawChannelSummary {
  readonly id: string;
  readonly channelCode: string;
  readonly channelName: string;
  readonly drawTime: string;
  readonly cutoffTime: string;
  readonly timezone: string;
  readonly daysOfWeek?: string | null;
  readonly active: boolean;
  readonly resultSlotActive?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminDrawChannelsApiService {
  private readonly backend = inject(TchBackendClient);

  getDrawChannelProviders(options?: TchRequestOptions): Observable<DrawChannelProviderView[]> {
    return this.backend
      .get<TenantDrawChannelSummary[]>('/tenant/draw-channels', {
        ...options,
        params: {
          ...(isRecordParams(options?.params) ? options.params : {}),
          activeOnly: 'false',
        },
      })
      .pipe(map(channels => this.toProviders(channels)));
  }

  setChannelActive(
    channelId: string,
    active: boolean,
    options?: TchRequestOptions,
  ): Observable<unknown> {
    return this.backend.patch<unknown>(
      `/tenant/draw-channels/${channelId}/active`,
      { active },
      options,
    );
  }

  getChannelDetail(
    channelId: string,
    options?: TchRequestOptions,
  ): Observable<DrawChannelDetailView> {
    return this.backend.get<DrawChannelDetailView>(
      `/tenant/draw-channels/${channelId}`,
      options,
    );
  }

  updateChannel(
    channelId: string,
    request: UpdateTenantDrawChannelRequest,
    options?: TchRequestOptions,
  ): Observable<DrawChannelDetailView> {
    return this.backend.put<DrawChannelDetailView>(
      `/tenant/draw-channels/${channelId}`,
      request,
      options,
    );
  }

  private toProviders(channels: readonly TenantDrawChannelSummary[]): DrawChannelProviderView[] {
    const groups = new Map<string, TenantDrawChannelSummary[]>();
    for (const channel of channels) {
      const identity = consoleDrawIdentity({
        channelCode: channel.channelCode,
        channelName: channel.channelName,
      });
      const providerCode = identity.providerCode ?? providerCodeFromChannel(channel.channelCode) ?? channel.channelCode;
      groups.set(providerCode, [...(groups.get(providerCode) ?? []), channel]);
    }

    return Array.from(groups.entries())
      .map(([providerCode, group]) => this.toProvider(providerCode, group))
      .sort((a, b) => {
        const byActive = Number(b.activeChannelCount ?? 0) - Number(a.activeChannelCount ?? 0);
        if (byActive !== 0) return byActive;
        return a.providerLabel.localeCompare(b.providerLabel);
      });
  }

  private toProvider(
    providerCode: string,
    channels: readonly TenantDrawChannelSummary[],
  ): DrawChannelProviderView {
    const slots = channels
      .map(channel => this.toSlot(providerCode, channel))
      .sort((a, b) => Number(b.enabled) - Number(a.enabled) || (a.drawTime ?? '').localeCompare(b.drawTime ?? ''));
    const activeChannelCount = slots.filter(slot => slot.enabled).length;
    const tenantStatus = activeChannelCount > 0 ? 'ACTIVE' : 'INACTIVE';
    const identity = consoleDrawIdentity({ providerCode });
    const firstTimezone = channels.find(channel => channel.timezone)?.timezone ?? providerCode;

    return {
      providerCode,
      providerLabel: identity.providerName ?? providerCode,
      timezone: firstTimezone,
      profileLabelKey: 'admin.drawChannels.profile.haitiLottery',
      tenantStatus,
      resultAcquisition: {
        mode: activeChannelCount > 0 ? 'MANUAL' : 'UNCONFIGURED',
        source: activeChannelCount > 0 ? 'MANUAL' : null,
        sourceStatus: activeChannelCount > 0 ? 'OK' : 'UNCONFIGURED',
      },
      configuredChannelCount: slots.length,
      activeChannelCount,
      slots,
      readiness: {
        status: tenantStatus === 'ACTIVE' ? 'READY' : 'TODO',
        label:
          tenantStatus === 'ACTIVE'
            ? 'admin.drawChannels.readiness.ready'
            : 'admin.drawChannels.readiness.todo',
        reason: tenantStatus === 'ACTIVE' ? null : 'admin.drawChannels.readiness.reason.noActiveChannels',
      },
    };
  }

  private toSlot(
    providerCode: string,
    channel: TenantDrawChannelSummary,
  ): DrawChannelSlotConfigView {
    const identity = consoleDrawIdentity({
      providerCode,
      channelCode: channel.channelCode,
      channelName: channel.channelName,
      localTimeLabel: channel.drawTime,
    });

    return {
      channelId: channel.id,
      slotKey: identity.slotKey ?? channel.channelCode,
      label: identity.channelName ?? channel.channelName ?? channel.channelCode,
      enabled: channel.active,
      resultSlotActive: channel.resultSlotActive,
      drawTime: channel.drawTime,
      cutoffTime: channel.cutoffTime,
      daysOfWeek: channel.daysOfWeek,
    };
  }
}

function isRecordParams(
  params: TchRequestOptions['params'] | undefined,
): params is Record<string, string | readonly string[]> {
  return params !== undefined && !(typeof params === 'object' && 'append' in params);
}

function providerCodeFromChannel(channelCode: string): string | null {
  const prefix = channelCode.trim().split(/[_-]/)[0];
  return prefix ? prefix.toUpperCase() : null;
}
