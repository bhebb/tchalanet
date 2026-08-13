import type {
  ConsoleDrawResultAcquisitionMode,
  ConsoleDrawResultSourceKind,
  ConsoleDrawResultSourceStatus,
} from '@tch/web/console';

export type UsLotteryProviderCode = string;

export type DrawChannelProviderTenantStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'NEEDS_CONFIG'
  | 'UNAVAILABLE';

export type DrawResultAcquisitionMode = ConsoleDrawResultAcquisitionMode;

export type DrawResultSourceStatus = ConsoleDrawResultSourceStatus;

export type DrawResultSourceKind = ConsoleDrawResultSourceKind;

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
  readonly resultSlotActive?: boolean;
  readonly drawTime?: string | null;
  readonly cutoffTime?: string | null;
  readonly salesCutoffMinutes?: number | null;
  readonly daysOfWeek?: string | null;
  readonly defaultSource?: DrawChannelSource | null;
}

export type DrawChannelSource =
  | 'SYSTEM'
  | 'AUTO'
  | 'EXTERNAL'
  | 'US_LOTTERY'
  | 'NY_OPEN_DATA'
  | 'FL_APIM'
  | 'MANUAL'
  | 'ADMIN_OVERRIDE';

export type DrawChannelWeekDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface DrawChannelDetailView {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly label?: string | null;
  readonly timezone?: string | null;
  readonly drawTime?: string | null;
  readonly salesOpenTime?: string | null;
  readonly cutoffSec?: number | null;
  readonly daysOfWeek?: readonly DrawChannelWeekDay[] | null;
  readonly active: boolean;
  readonly sortOrder?: number | null;
  readonly period?: string | null;
  readonly notes?: string | null;
  readonly resultSlotId?: string | null;
  readonly defaultSource?: DrawChannelSource | null;
  readonly resultSlotKey?: string | null;
  readonly resultProvider?: string | null;
  readonly resultProviderSlotCode?: string | null;
  readonly resultSlotDaysOfWeek?: string | null;
  readonly resultSlotActive?: boolean | null;
}

export interface UpdateTenantDrawChannelRequest {
  readonly name?: string;
  readonly label?: string | null;
  readonly timezone?: string | null;
  readonly drawTime?: string | null;
  readonly cutoffSec?: number | null;
  readonly daysOfWeek?: readonly DrawChannelWeekDay[] | null;
  readonly active?: boolean;
  readonly sortOrder?: number | null;
  readonly period?: string | null;
  readonly notes?: string | null;
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
  readonly profileLabelKey?: string;
  readonly tenantStatus: DrawChannelProviderTenantStatus;
  readonly resultAcquisition: DrawResultAcquisitionView;
  readonly defaultSalesCutoffMinutes?: number | null;
  readonly configuredChannelCount?: number;
  readonly activeChannelCount?: number;
  readonly offeredGameCount?: number;
  readonly saleReadyGameCount?: number;
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
