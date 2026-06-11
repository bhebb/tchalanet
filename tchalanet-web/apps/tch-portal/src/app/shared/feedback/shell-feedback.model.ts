export type ShellFeedbackSeverity = 'info' | 'warn' | 'error';
export type ShellFeedbackVerbosity = 'minimal' | 'standard' | 'verbose';

export interface ShellFeedbackItem {
  readonly id: string;
  readonly severity: ShellFeedbackSeverity;
  readonly title: string;
  readonly message: string;
  readonly source?: string;
  readonly traceId?: string;
  readonly status?: number;
  readonly copyText?: string;
  readonly dismissible: boolean;
}

export interface AddShellFeedbackInput {
  readonly severity: ShellFeedbackSeverity;
  readonly title: string;
  readonly message: string;
  readonly source?: string;
  readonly traceId?: string;
  readonly status?: number;
  readonly copyText?: string;
  readonly dismissible?: boolean;
}
