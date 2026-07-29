export type ApiStatus =
  | 'SUCCESS'
  | 'CREATED'
  | 'SUCCESS_WITH_WARNINGS'
  | 'PENDING'
  | 'PARTIAL'
  | 'ACCEPTED';

export interface TchDiagnosticInfo {
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
}

export type NoticeSeverity = 'INFO' | 'WARN' | 'ERROR' | 'info' | 'success' | 'warning' | 'error';

export interface ApiNoticeSource {
  readonly source: string;
  readonly service?: string;
  readonly operation?: string;
}

export interface ApiNoticeTrace extends TchDiagnosticInfo {
  readonly errorId?: string;
}

export interface ApiNotice {
  readonly code: string;
  /** Diagnostic-only during migration. Clients translate the stable code. */
  readonly message?: string | null;
  readonly domain?: string | null;
  readonly severity: NoticeSeverity;
  readonly kind?: 'BUSINESS' | 'DEGRADATION' | 'INFORMATION' | string;
  readonly retryPolicy?: string;
  readonly retryable?: boolean;
  readonly source?: ApiNoticeSource;
  readonly target?: string;
  readonly params?: Readonly<Record<string, string | number | boolean>>;
  readonly trace?: ApiNoticeTrace;
  /** @deprecated Compatibility bridge for producers not yet emitting structured fields. */
  readonly meta?: Readonly<Record<string, unknown>> | null;
}

export interface ServiceStatus {
  readonly service: string;
  readonly status: 'UP' | 'DOWN' | 'DEGRADED' | string;
  readonly message?: string | null;
}

export interface ApiResponse<T> {
  readonly status: ApiStatus;
  readonly data: T;
  readonly notices: readonly ApiNotice[];
  readonly services?: readonly ServiceStatus[];
  /** @deprecated Server emits `services`; kept during web migration. */
  readonly serviceHealth?: readonly ServiceStatus[];
  readonly correlationId?: string;
  readonly trace?: TchDiagnosticInfo;
}

export interface ProblemFieldViolation {
  readonly code?: string;
  readonly field: string;
  readonly target?: string;
  /** Diagnostic-only during migration. */
  readonly message?: string;
}

export interface ProblemDetail {
  readonly type?: string;
  readonly title: string;
  readonly status: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly correlationId?: string;
  readonly errors?: Readonly<Record<string, readonly string[]>>;
  readonly violations?: readonly ProblemFieldViolation[];
  // Trace context — populated from response headers or ProblemDetail body fields
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
  readonly errorId?: string;
  readonly code?: string;
  readonly category?: string;
  readonly retryPolicy?: string;
  readonly retryable?: boolean;
  readonly params?: Readonly<Record<string, string | number | boolean>>;
}

export interface TchPage<T> {
  readonly items: T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
  readonly last?: boolean;
  readonly hasNext?: boolean;
  readonly hasPrevious?: boolean;
}

/**
 * Shared fallback for a paginated request's `size` param, and for a table/list component's own
 * `pageSize` input when no page has loaded yet. Every list across admin-portal and
 * platform-portal had independently invented this same number (or drifted from it, e.g. one
 * request defaulting to 100 while its table defaulted to 20) — one exported constant so a change
 * has one source, and a new page or the request/table pair can't drift apart from each other.
 *
 * Always prefer the value the backend actually echoed back (`TchPage.size`) once a response has
 * loaded — this constant is only the fallback before that, or for requests with no reason to
 * differ from the default.
 */
export const TCH_DEFAULT_PAGE_SIZE = 20;

export interface TchBackendPage<T> {
  readonly items?: readonly T[];
  readonly content?: readonly T[];
  readonly total?: number;
  readonly totalElements?: number;
  readonly totalPages?: number;
  readonly page?: number;
  readonly number?: number;
  readonly size?: number;
  readonly last?: boolean;
  readonly first?: boolean;
  readonly hasNext?: boolean;
  readonly hasPrevious?: boolean;
}
