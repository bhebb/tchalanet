export type OperationalContextStatus = 'MISSING' | 'PARTIAL' | 'READY';

export interface OperationalContextView {
  readonly terminalId?: string;
  readonly terminalCode?: string;
  readonly outletId?: string;
  readonly outletName?: string;
  readonly salesSessionId?: string;
  readonly status: OperationalContextStatus;
  readonly source: string;
}
