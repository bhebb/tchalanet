import { WebAppError } from '@tch/api';

export interface TchErrorViewModel {
  readonly title: string;
  readonly message: string;
  readonly severity: WebAppError['severity'];
  readonly code?: string;
}

export type TchError = WebAppError;
export type TchErrorSeverity = WebAppError['severity'];
export type TchErrorSurface = WebAppError['surface'];

export type ErrorViewModel = TchErrorViewModel;
