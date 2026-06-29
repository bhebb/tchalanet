import type { VerificationStatus } from '@tch/page-model';

import type { PublicTicketVerificationResponse } from './public-ticket.service';

export type CheckState =
  | { readonly kind: 'default' }
  | { readonly kind: 'loading' }
  | {
      readonly kind: 'result';
      readonly status: VerificationStatus;
      readonly data: PublicTicketVerificationResponse | null;
    };

export interface VerificationCopy {
  readonly icon: string;
  readonly tone: 'warning' | 'neutral' | 'success' | 'danger';
  readonly titleKey: string;
  readonly bodyKey: string;
}
