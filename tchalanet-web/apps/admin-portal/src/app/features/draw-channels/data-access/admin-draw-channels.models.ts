export type UsLotteryProviderCode =
  | 'NY'
  | 'FL'
  | 'GA'
  | 'TN'
  | 'TX'
  | 'PA'
  | 'NJ'
  | 'CA'
  | 'MO'
  | 'IL'
  | 'MI'
  | 'OH'
  | 'MN';

export type DrawChannelProviderTenantStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'NEEDS_CONFIG'
  | 'UNAVAILABLE';

export type DrawResultAcquisitionMode = 'AUTO' | 'MANUAL' | 'UNCONFIGURED';

export type DrawResultSourceStatus = 'OK' | 'PENDING' | 'ERROR' | 'DISABLED' | 'UNCONFIGURED';

export type DrawResultSourceKind = 'API' | 'RSS' | 'BATCH' | 'MANUAL';

export interface DrawResultAcquisitionView {
  readonly mode: DrawResultAcquisitionMode;
  readonly sourceStatus: DrawResultSourceStatus;
  readonly source?: DrawResultSourceKind | null;
  readonly lastSyncAt?: string | null;
  readonly nextSyncAt?: string | null;
  readonly lastManualEntryAt?: string | null;
  readonly lastAttemptAt?: string | null;
  readonly lastError?: string | null;
}

export interface DrawChannelSlotConfigView {
  readonly channelId?: string | null;
  readonly slotKey: string;
  readonly label: string;
  readonly enabled: boolean;
  readonly drawTime?: string | null;
  readonly salesCutoffMinutes?: number | null;
}

export interface DrawChannelProviderReadinessView {
  readonly status: 'READY' | 'TODO' | 'WARNING' | 'BLOCKED';
  readonly label: string;
  readonly reason?: string | null;
}

export interface DrawChannelProviderView {
  readonly providerCode: UsLotteryProviderCode;
  readonly providerLabel: string;
  readonly timezone: string;
  readonly tenantStatus: DrawChannelProviderTenantStatus;
  readonly resultAcquisition: DrawResultAcquisitionView;
  readonly defaultSalesCutoffMinutes?: number | null;
  readonly slots: readonly DrawChannelSlotConfigView[];
  readonly readiness: DrawChannelProviderReadinessView;
}

export interface UpdateDrawChannelProviderConfigRequest {
  readonly enabled: boolean;
  readonly resultAcquisitionMode: DrawResultAcquisitionMode;
  readonly defaultSalesCutoffMinutes?: number | null;
  readonly slots: readonly UpdateDrawChannelSlotConfigRequest[];
}

export interface UpdateDrawChannelSlotConfigRequest {
  readonly slotKey: string;
  readonly enabled: boolean;
  readonly drawTime?: string | null;
  readonly salesCutoffMinutes?: number | null;
}
