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
