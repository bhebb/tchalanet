import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import {
  DrawChannelSlotConfigView,
  DrawChannelProviderView,
  DrawResultAcquisitionMode,
  UpdateDrawChannelProviderConfigRequest,
  UsLotteryProviderCode,
} from './admin-draw-channels.models';

// TODO(backend): replace mock with real HTTP calls to /admin/draw-channels once endpoint is ready.
// Expected backend shape: TenantDrawChannelProviderView[] grouped by provider/state.
function slot(slotKey: string, label: string, drawTime: string, enabled = false): DrawChannelSlotConfigView {
  return { slotKey, label, enabled, drawTime, salesCutoffMinutes: enabled ? 10 : null };
}

function inactiveProvider(
  providerCode: UsLotteryProviderCode,
  providerLabel: string,
  timezone: string,
  slots: readonly DrawChannelSlotConfigView[],
  resultAcquisitionMode: DrawResultAcquisitionMode = 'UNCONFIGURED',
): DrawChannelProviderView {
  return {
    providerCode,
    providerLabel,
    timezone,
    tenantStatus: 'INACTIVE',
    resultAcquisition: { mode: resultAcquisitionMode, sourceStatus: 'UNCONFIGURED' },
    defaultSalesCutoffMinutes: null,
    slots,
    readiness: {
      status: 'TODO',
      label: 'Configuration incomplète',
      reason: resultAcquisitionMode === 'UNCONFIGURED' ? 'Mode résultat non défini' : null,
    },
  };
}

const MOCK_PROVIDERS: DrawChannelProviderView[] = [
  {
    providerCode: 'NY',
    providerLabel: 'New York',
    timezone: 'America/New_York',
    tenantStatus: 'ACTIVE',
    resultAcquisition: {
      mode: 'AUTO',
      sourceStatus: 'OK',
      source: 'RSS',
      lastSyncAt: '14:04',
      nextSyncAt: '22:30',
    },
    defaultSalesCutoffMinutes: 10,
    slots: [
      { slotKey: 'MIDDAY',  label: 'Midday',  enabled: true,  drawTime: '14:30', salesCutoffMinutes: 10 },
      { slotKey: 'EVENING', label: 'Evening', enabled: true,  drawTime: '22:30', salesCutoffMinutes: 10 },
    ],
    readiness: { status: 'READY', label: 'Prêt pour la vente' },
  },
  {
    providerCode: 'FL',
    providerLabel: 'Florida',
    timezone: 'America/New_York',
    tenantStatus: 'ACTIVE',
    resultAcquisition: {
      mode: 'AUTO',
      sourceStatus: 'OK',
      source: 'API',
      lastSyncAt: '13:50',
      nextSyncAt: '22:45',
    },
    defaultSalesCutoffMinutes: 10,
    slots: [
      { slotKey: 'MIDDAY',  label: 'Midday',  enabled: true,  drawTime: '13:30', salesCutoffMinutes: 10 },
      { slotKey: 'EVENING', label: 'Evening', enabled: true,  drawTime: '22:45', salesCutoffMinutes: 10 },
    ],
    readiness: { status: 'READY', label: 'Prêt pour la vente' },
  },
  inactiveProvider('GA', 'Georgia', 'America/New_York', [
    slot('MIDDAY', 'Midday', '12:29'),
    slot('EVENING', 'Evening', '18:59'),
    slot('NIGHT', 'Night', '23:34'),
  ]),
  inactiveProvider('TX', 'Texas', 'America/Chicago', [
    slot('MORNING', 'Morning', '10:00'),
    slot('DAY', 'Day', '12:27'),
    slot('EVENING', 'Evening', '18:00'),
    slot('NIGHT', 'Night', '22:12'),
  ]),
  inactiveProvider('TN', 'Tennessee', 'America/Chicago', [
    slot('MIDDAY', 'Midday', '12:29'),
    slot('EVENING', 'Evening', '22:00'),
  ], 'MANUAL'),
  inactiveProvider('PA', 'Pennsylvania', 'America/New_York', [
    slot('MIDDAY', 'Midday', '13:35'),
    slot('EVENING', 'Evening', '19:00'),
  ]),
  inactiveProvider('NJ', 'New Jersey', 'America/New_York', [
    slot('MIDDAY', 'Midday', '12:59'),
    slot('EVENING', 'Evening', '22:57'),
  ]),
  inactiveProvider('CA', 'California', 'America/Los_Angeles', [
    slot('MIDDAY', 'Midday', '13:00'),
    slot('EVENING', 'Evening', '18:30'),
  ]),
  inactiveProvider('OH', 'Ohio', 'America/New_York', [
    slot('MIDDAY', 'Midday', '12:29'),
    slot('EVENING', 'Evening', '19:29'),
  ]),
  inactiveProvider('MI', 'Michigan', 'America/Detroit', [
    slot('MIDDAY', 'Midday', '12:59'),
    slot('EVENING', 'Evening', '19:29'),
  ]),
  inactiveProvider('IL', 'Illinois', 'America/Chicago', [
    slot('MIDDAY', 'Midday', '12:40'),
    slot('EVENING', 'Evening', '21:22'),
  ], 'MANUAL'),
  inactiveProvider('MO', 'Missouri', 'America/Chicago', [
    slot('MIDDAY', 'Midday', '12:45'),
    slot('EVENING', 'Evening', '21:00'),
  ], 'MANUAL'),
  inactiveProvider('MN', 'Minnesota', 'America/Chicago', [
    slot('EVENING', 'Evening', '18:17'),
  ], 'MANUAL'),
];

@Injectable({ providedIn: 'root' })
export class AdminDrawChannelsApiService {
  private providers: DrawChannelProviderView[] = [...MOCK_PROVIDERS];

  getDrawChannelProviders(): Observable<DrawChannelProviderView[]> {
    return of(this.providers).pipe(delay(400));
  }

  updateDrawChannelProviderConfig(
    providerCode: UsLotteryProviderCode,
    request: UpdateDrawChannelProviderConfigRequest,
    options?: { readonly suppressShellFeedback?: boolean },
  ): Observable<DrawChannelProviderView> {
    void options;
    // TODO(backend): POST /admin/draw-channels/:providerCode/config
    const provider = this.providers.find(item => item.providerCode === providerCode) ?? this.providers[0];
    const updatedProvider: DrawChannelProviderView = {
      ...provider,
      tenantStatus: request.enabled ? 'ACTIVE' : 'INACTIVE',
      resultAcquisition: {
        ...provider.resultAcquisition,
        mode: request.resultAcquisitionMode,
      },
      defaultSalesCutoffMinutes: request.defaultSalesCutoffMinutes ?? null,
      slots: provider.slots.map(slot => {
        const update = request.slots.find(item => item.slotKey === slot.slotKey);
        return update
          ? {
              ...slot,
              enabled: update.enabled,
              drawTime: update.drawTime,
              salesCutoffMinutes: update.salesCutoffMinutes,
            }
          : slot;
      }),
    };
    this.providers = this.providers.map(item => item.providerCode === providerCode ? updatedProvider : item);
    return of(updatedProvider).pipe(delay(200));
  }
}
