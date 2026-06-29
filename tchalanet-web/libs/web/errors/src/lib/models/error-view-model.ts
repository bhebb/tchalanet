import { WebAppError } from '@tch/api';

export interface TchErrorViewModel {
  readonly title: string;
  readonly message: string;
  readonly severity: WebAppError['severity'];
  readonly surface?: WebAppError['surface'];
  readonly placement?: WebAppError['placement'];
  readonly code?: string;
  readonly status?: number;
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
  readonly errorId?: string;
  readonly source?: string;
  readonly target?: string;
  readonly field?: string;
  readonly retryable?: boolean;
  readonly dedupeKey?: string;
}

export type TchError = WebAppError;
export type TchErrorSeverity = WebAppError['severity'];
export type TchErrorSurface = WebAppError['surface'];

export type ErrorViewModel = TchErrorViewModel;
