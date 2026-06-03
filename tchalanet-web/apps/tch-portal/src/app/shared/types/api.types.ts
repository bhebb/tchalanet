export type ApiStatus = 'SUCCESS' | 'WARNING' | 'ERROR';

export type NoticeSeverity = 'info' | 'success' | 'warning' | 'error';

export interface ApiNotice {
  readonly code: string;
  readonly severity: NoticeSeverity;
  readonly message: string;
  readonly target?: string;
}

export interface ServiceStatus {
  readonly code: string;
  readonly label: string;
  readonly healthy: boolean;
}

export interface ServiceHealth {
  readonly service: string;
  readonly status: ServiceStatus;
  readonly checkedAt: string;
  readonly details?: Readonly<Record<string, unknown>>;
}

export interface ApiResponse<T> {
  readonly status: ApiStatus;
  readonly data: T;
  readonly notices: readonly ApiNotice[];
  readonly serviceHealth?: readonly ServiceHealth[];
  readonly correlationId?: string;
}

export interface ProblemDetail {
  readonly type?: string;
  readonly title: string;
  readonly status: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly correlationId?: string;
  readonly errors?: Readonly<Record<string, readonly string[]>>;
}

export interface TchPage<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
