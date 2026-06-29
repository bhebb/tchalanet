export type BootstrapStatus = 'idle' | 'loading' | 'ready' | 'partial' | 'error';

export interface PageModelRef {
  readonly route: string;
  readonly endpoint: string;
}

export interface RuntimeBootstrapNotice {
  readonly code: string;
  readonly message: string;
  readonly level: 'INFO' | 'WARNING' | 'ERROR';
}
