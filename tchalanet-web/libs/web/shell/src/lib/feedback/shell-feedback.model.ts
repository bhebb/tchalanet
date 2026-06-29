export type ShellFeedbackSeverity = 'info' | 'warn' | 'error';
export type ShellFeedbackVerbosity = 'minimal' | 'standard' | 'verbose';

export interface ShellFeedbackItem {
  readonly id: string;
  readonly dedupeKey: string;
  readonly severity: ShellFeedbackSeverity;
  readonly title: string;
  readonly message: string;
  readonly source?: string;
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
  readonly errorId?: string;
  readonly status?: number;
  readonly copyText?: string;
  readonly dismissible: boolean;
  readonly repeatCount: number;
}

export interface AddShellFeedbackInput {
  readonly dedupeKey?: string;
  readonly severity: ShellFeedbackSeverity;
  readonly title: string;
  readonly message: string;
  readonly source?: string;
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
  readonly errorId?: string;
  readonly status?: number;
  readonly copyText?: string;
  readonly dismissible?: boolean;
}
