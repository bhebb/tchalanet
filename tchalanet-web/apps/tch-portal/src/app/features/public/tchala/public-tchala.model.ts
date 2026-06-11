export interface TchalaDisplayEntry {
  readonly id: string;
  readonly icon: string;
  readonly term: string;
  readonly description: string;
  readonly numbers: readonly string[];
}

export type FormState = 'idle' | 'submitting' | 'success' | 'error' | 'limit_exceeded';
