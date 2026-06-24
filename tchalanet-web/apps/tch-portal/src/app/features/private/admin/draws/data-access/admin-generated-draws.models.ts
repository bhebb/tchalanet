export type GeneratedDrawSalesStatus = 'UPCOMING' | 'OPEN' | 'CLOSED' | 'CANCELLED';

export type GeneratedDrawResultStatus =
  | 'NOT_DUE'
  | 'EXPECTED'
  | 'MISSING'
  | 'PROVISIONAL'
  | 'CONFIRMED'
  | 'SOURCE_ERROR';

export type GeneratedDrawResultMode = 'AUTO' | 'MANUAL';

export type GeneratedDrawPublicationStatus = 'NOT_PUBLISHED' | 'PUBLISHED';

export interface GeneratedDrawView {
  readonly drawId: string;
  readonly drawChannelId: string;
  readonly providerCode: string;
  readonly providerLabel: string;
  readonly slotKey: string;
  readonly slotLabel: string;
  readonly label: string;
  readonly businessDate: string;
  readonly scheduledAt: string;
  readonly timezone: string;
  readonly salesStatus: GeneratedDrawSalesStatus;
  readonly resultStatus: GeneratedDrawResultStatus;
  readonly resultMode: GeneratedDrawResultMode;
  readonly publicationStatus?: GeneratedDrawPublicationStatus | null;
  readonly numbers?: string[] | null;
  readonly sourceError?: { readonly message: string; readonly occurredAt?: string | null } | null;
  /** Raw backend DrawStatus — used to determine available lifecycle actions. */
  readonly lifecycleStatus?: string;
}

export interface GeneratedDrawGroup {
  readonly date: string;
  readonly draws: readonly GeneratedDrawView[];
}

export interface GeneratedDrawsQuery {
  readonly datePreset?: 'TODAY' | 'TOMORROW' | 'THIS_WEEK';
  readonly status?: string | null;
  readonly provider?: string | null;
  readonly q?: string | null;
  readonly page?: number;
  readonly size?: number;
}

export interface PagedResult<T> {
  readonly content: T[];
  readonly totalElements: number;
  readonly page: number;
  readonly size: number;
}

export interface PlanNextDrawsRequest {
  readonly daysAhead?: number;
  readonly providerCode?: string | null;
}

export interface PlanNextDrawsResult {
  readonly createdCount: number;
  readonly skippedCount: number;
  readonly rangeStart: string;
  readonly rangeEnd: string;
}

export type DrawStatusFilter = 'all' | 'OPEN' | 'EXPECTED' | 'MISSING' | 'CONFIRMED' | 'SOURCE_ERROR';
export type DatePreset = 'TODAY' | 'TOMORROW' | 'THIS_WEEK';

export type DrawResultSaveMode = 'provisional' | 'confirmed';

export interface SaveDrawResultRequest {
  readonly drawId: string;
  readonly numbers: readonly string[];
  readonly note: string;
  readonly mode: DrawResultSaveMode;
}

export type DrawLifecycleAction = 'cancel' | 'lock' | 'unlock' | 'archive';
