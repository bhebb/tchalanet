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

export interface ApiNotice {
  readonly code: string;
  readonly message: string;
  readonly domain?: string | null;
  readonly severity: NoticeSeverity;
  readonly meta?: Readonly<Record<string, unknown>> | null;
  readonly target?: string;
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
