import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import {
  DrawChannelProviderView,
  UpdateDrawChannelProviderConfigRequest,
  UsLotteryProviderCode,
} from './admin-draw-channels.models';

// TODO(backend): replace mock with real HTTP calls to /admin/draw-channels once endpoint is ready.
// Expected backend shape: TenantDrawChannelProviderView[] grouped by provider/state.
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
      mode: 'MANUAL',
      sourceStatus: 'OK',
      lastManualEntryAt: '13:15',
    },
    defaultSalesCutoffMinutes: 10,
    slots: [
      { slotKey: 'MIDDAY',  label: 'Midday',  enabled: true,  drawTime: '13:30', salesCutoffMinutes: 10 },
      { slotKey: 'EVENING', label: 'Evening', enabled: true,  drawTime: '21:00', salesCutoffMinutes: 10 },
    ],
    readiness: { status: 'READY', label: 'Prêt pour la vente' },
  },
  {
    providerCode: 'TX',
    providerLabel: 'Texas',
    timezone: 'America/Chicago',
    tenantStatus: 'ACTIVE',
    resultAcquisition: {
      mode: 'AUTO',
      sourceStatus: 'ERROR',
      source: 'RSS',
      lastAttemptAt: '12:27',
      lastError: 'Connection timeout',
    },
    defaultSalesCutoffMinutes: 10,
    slots: [
      { slotKey: 'MORNING', label: 'Morning', enabled: true, drawTime: '10:00', salesCutoffMinutes: 10 },
      { slotKey: 'DAY',     label: 'Day',     enabled: true, drawTime: '12:27', salesCutoffMinutes: 10 },
    ],
    readiness: { status: 'WARNING', label: 'Vente configurée · Source à vérifier' },
  },
  {
    providerCode: 'MN',
    providerLabel: 'Minnesota',
    timezone: 'America/Chicago',
    tenantStatus: 'INACTIVE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'UNCONFIGURED' },
    defaultSalesCutoffMinutes: null,
    slots: [
      { slotKey: 'MIDDAY',  label: 'Midday',  enabled: false, drawTime: null },
      { slotKey: 'EVENING', label: 'Evening', enabled: false, drawTime: null },
    ],
    readiness: { status: 'TODO', label: 'Configuration incomplète', reason: 'Mode résultat non défini' },
  },
  {
    providerCode: 'GA',
    providerLabel: 'Georgia',
    timezone: 'America/New_York',
    tenantStatus: 'INACTIVE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'UNCONFIGURED' },
    defaultSalesCutoffMinutes: null,
    slots: [
      { slotKey: 'MIDDAY',  label: 'Midday',  enabled: false, drawTime: null },
      { slotKey: 'EVENING', label: 'Evening', enabled: false, drawTime: null },
      { slotKey: 'NIGHT',   label: 'Night',   enabled: false, drawTime: null },
    ],
    readiness: { status: 'TODO', label: 'Configuration incomplète' },
  },
  {
    providerCode: 'TN',
    providerLabel: 'Tennessee',
    timezone: 'America/Chicago',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'PA',
    providerLabel: 'Pennsylvania',
    timezone: 'America/New_York',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'NJ',
    providerLabel: 'New Jersey',
    timezone: 'America/New_York',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'CA',
    providerLabel: 'California',
    timezone: 'America/Los_Angeles',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'MO',
    providerLabel: 'Missouri',
    timezone: 'America/Chicago',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'IL',
    providerLabel: 'Illinois',
    timezone: 'America/Chicago',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'MI',
    providerLabel: 'Michigan',
    timezone: 'America/Detroit',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
  {
    providerCode: 'OH',
    providerLabel: 'Ohio',
    timezone: 'America/New_York',
    tenantStatus: 'UNAVAILABLE',
    resultAcquisition: { mode: 'UNCONFIGURED', sourceStatus: 'DISABLED' },
    defaultSalesCutoffMinutes: null,
    slots: [],
    readiness: { status: 'BLOCKED', label: 'Non disponible' },
  },
];

@Injectable({ providedIn: 'root' })
export class AdminDrawChannelsApiService {
  getDrawChannelProviders(): Observable<DrawChannelProviderView[]> {
    return of(MOCK_PROVIDERS).pipe(delay(400));
  }

  updateDrawChannelProviderConfig(
    providerCode: UsLotteryProviderCode,
    request: UpdateDrawChannelProviderConfigRequest,
  ): Observable<DrawChannelProviderView> {
    // TODO(backend): POST /admin/draw-channels/:providerCode/config
    const provider = MOCK_PROVIDERS.find(item => item.providerCode === providerCode) ?? MOCK_PROVIDERS[0];
    const updatedProvider: DrawChannelProviderView = {
      ...provider,
      tenantStatus: request.enabled ? 'ACTIVE' : 'INACTIVE',
      resultAcquisition: {
        ...provider.resultAcquisition,
        mode: request.resultAcquisitionMode,
      },
      defaultSalesCutoffMinutes: request.defaultSalesCutoffMinutes ?? provider.defaultSalesCutoffMinutes,
    };
    return of(updatedProvider).pipe(delay(200));
  }
}
