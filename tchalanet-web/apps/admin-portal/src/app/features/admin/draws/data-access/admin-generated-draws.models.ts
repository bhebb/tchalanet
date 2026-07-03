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
  readonly cutoffAt?: string | null;
  readonly timezone: string;
  readonly salesStatus: GeneratedDrawSalesStatus;
  readonly resultStatus: GeneratedDrawResultStatus;
  readonly resultMode: GeneratedDrawResultMode;
  readonly resultId?: string | null;
  readonly publicationStatus?: GeneratedDrawPublicationStatus | null;
  readonly numbers?: string[] | null;
  readonly fetchedAt?: string | null;
  readonly sourceError?: { readonly message: string; readonly occurredAt?: string | null } | null;
  /** Raw backend DrawStatus — used to determine available lifecycle actions. */
  readonly lifecycleStatus?: string;
}

export interface GeneratedDrawGroup {
  readonly date: string;
  readonly draws: readonly GeneratedDrawView[];
}

export interface GeneratedDrawsQuery {
  readonly datePreset?: DatePreset;
  readonly from?: string | null;
  readonly to?: string | null;
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

export type DrawStatusFilter =
  | 'all'
  | 'SCHEDULED'
  | 'OPEN'
  | 'LOCKED'
  | 'CLOSED'
  | 'RESULTED'
  | 'SETTLED'
  | 'CANCELLED'
  | 'ARCHIVED'
  | 'PAST'
  | 'EXPECTED'
  | 'MISSING'
  | 'PROVISIONAL'
  | 'CONFIRMED'
  | 'SOURCE_ERROR'
  | 'NOT_DUE';
export type DatePreset = 'LAST_48H' | 'TODAY' | 'TOMORROW' | 'THIS_WEEK';

export type DrawResultSaveMode = 'provisional' | 'confirmed';

export interface SaveDrawResultRequest {
  readonly drawId: string;
  readonly numbers: readonly string[];
  readonly note: string;
  readonly mode: DrawResultSaveMode;
  readonly force?: boolean;
}

export type DrawLifecycleAction = 'open' | 'close' | 'cancel' | 'lock' | 'unlock' | 'archive';

export interface GeneratedDrawDateTimeParts {
  readonly year: string;
  readonly month: string;
  readonly day: string;
  readonly hour: string;
  readonly minute: string;
  readonly second?: string;
}

const TIMEZONE_LABELS: Readonly<Record<string, string>> = {
  'America/New_York': 'ET',
  'America/Detroit': 'ET',
  'America/Chicago': 'CT',
  'America/Los_Angeles': 'PT',
  'America/Port-au-Prince': 'HT',
};

export function generatedDrawCloseTimestamp(
  draw: Pick<GeneratedDrawView, 'cutoffAt' | 'scheduledAt' | 'timezone'>,
): number | null {
  return generatedDrawDateTimeToEpochMs(draw.cutoffAt ?? draw.scheduledAt, draw.timezone);
}

export function isGeneratedDrawSellableNow(draw: GeneratedDrawView, nowMs = Date.now()): boolean {
  if (draw.salesStatus !== 'OPEN') return false;
  const closeAt = generatedDrawCloseTimestamp(draw);
  return closeAt != null && closeAt > nowMs;
}

export function generatedDrawCountdownMs(draw: GeneratedDrawView, nowMs = Date.now()): number | null {
  if (draw.salesStatus !== 'OPEN') return null;
  const closeAt = generatedDrawCloseTimestamp(draw);
  return closeAt == null ? null : closeAt - nowMs;
}

export function generatedDrawDateTimeToEpochMs(value: string, timezone: string): number | null {
  if (generatedDrawHasExplicitOffset(value)) {
    const epoch = Date.parse(value);
    return Number.isNaN(epoch) ? null : epoch;
  }

  const parts = generatedDrawLocalDateTimeParts(value);
  if (!parts) {
    const epoch = Date.parse(value);
    return Number.isNaN(epoch) ? null : epoch;
  }

  return generatedDrawZonedLocalTimeToEpochMs(
    Number(parts.year),
    Number(parts.month),
    Number(parts.day),
    Number(parts.hour),
    Number(parts.minute),
    Number(parts.second ?? '0'),
    timezone,
  );
}

export function generatedDrawLocalDateTime(value: string, timezone: string): string | null {
  const parts = generatedDrawHasExplicitOffset(value) ? null : generatedDrawLocalDateTimeParts(value);
  if (parts) return `${parts.day}/${parts.month}/${parts.year} ${parts.hour}:${parts.minute}`;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: timezone,
  });
}

export function generatedDrawLocalIsoDateTime(value: string, timezone: string): string | null {
  const parts = generatedDrawHasExplicitOffset(value) ? null : generatedDrawLocalDateTimeParts(value);
  if (parts) return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}`;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  const dateParts = new Intl.DateTimeFormat('fr-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: timezone,
  }).formatToParts(date);
  const pick = (type: string) => dateParts.find(part => part.type === type)?.value ?? '';
  return `${pick('year')}-${pick('month')}-${pick('day')} ${pick('hour')}:${pick('minute')}`;
}

export function generatedDrawLocalDateLabel(value: string, timezone: string): string | null {
  const parts = generatedDrawHasExplicitOffset(value) ? null : generatedDrawLocalDateTimeParts(value);
  if (parts) {
    const date = new Date(`${parts.year}-${parts.month}-${parts.day}T00:00:00`);
    return Number.isNaN(date.getTime())
      ? null
      : date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    timeZone: timezone,
  });
}

export function generatedDrawLocalTimeLabel(value: string, timezone: string): string | null {
  const parts = generatedDrawHasExplicitOffset(value) ? null : generatedDrawLocalDateTimeParts(value);
  if (parts) return `${parts.hour}:${parts.minute}`;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: timezone,
  }).format(date);
}

export function generatedDrawTimezoneShortLabel(timezone: string): string {
  const normalized = timezone?.trim();
  if (!normalized) return 'local';
  const parts = normalized.split('/');
  return TIMEZONE_LABELS[normalized] ?? parts[parts.length - 1]?.replace(/_/g, ' ') ?? normalized;
}

export function generatedDrawTimeWithZoneLabel(value: string, timezone: string): string | null {
  const time = generatedDrawLocalTimeLabel(value, timezone);
  return time ? `${time} ${generatedDrawTimezoneShortLabel(timezone)}` : null;
}

export function generatedDrawProviderAndTenantTimeLabel(
  value: string,
  providerTimezone: string,
  tenantTimezone: string,
): string | null {
  const providerTime = generatedDrawTimeWithZoneLabel(value, providerTimezone);
  if (!providerTime) return null;

  if (providerTimezone === tenantTimezone) return providerTime;

  const tenantTime = generatedDrawLocalTimeLabel(value, tenantTimezone);
  return tenantTime ? `${providerTime} · ${tenantTime} local` : providerTime;
}

export function generatedDrawTenantDateTimeLabel(value: string | null | undefined, timezone: string): string {
  if (!value) return '—';
  const local = generatedDrawLocalDateTime(value, timezone);
  return local ? `${local} local` : '—';
}

export function generatedDrawLocalDateTimeParts(value: string): GeneratedDrawDateTimeParts | null {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
  if (!match) return null;
  return {
    year: match[1],
    month: match[2],
    day: match[3],
    hour: match[4],
    minute: match[5],
    second: match[6],
  };
}

export function generatedDrawHasExplicitOffset(value: string): boolean {
  return /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value);
}

function generatedDrawZonedLocalTimeToEpochMs(
  year: number,
  month: number,
  day: number,
  hour: number,
  minute: number,
  second: number,
  timezone: string,
): number {
  const utcGuess = Date.UTC(year, month - 1, day, hour, minute, second);
  const offset = generatedDrawTimezoneOffsetMs(new Date(utcGuess), timezone);
  const first = utcGuess - offset;
  const correctedOffset = generatedDrawTimezoneOffsetMs(new Date(first), timezone);
  return utcGuess - correctedOffset;
}

function generatedDrawTimezoneOffsetMs(date: Date, timezone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const pick = (type: string) => Number(parts.find(part => part.type === type)?.value ?? 0);
  const asUtc = Date.UTC(
    pick('year'),
    pick('month') - 1,
    pick('day'),
    pick('hour'),
    pick('minute'),
    pick('second'),
  );
  return asUtc - date.getTime();
}
