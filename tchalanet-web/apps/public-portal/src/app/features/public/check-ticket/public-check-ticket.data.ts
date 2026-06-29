import type { VerificationStatus } from '@tch/page-model';

/** Format attendu : XXXX-XXXX (4 alphanum + tiret + 4 alphanum) */
export const CODE_PATTERN = /^[A-Z0-9]{4}-[A-Z0-9]{4}$/;

export const STAMP_LINES: Record<VerificationStatus, string[]> = {
  WINNING_PAYABLE:     ['GAGNANT', 'PAYABLE'],
  WINNING_PAID:        ['DÉJÀ', 'PAYÉ'],
  LOST:                ['NON', 'GAGNANT'],
  PENDING_RESULT:      ['EN', 'ATTENTE'],
  CANCELLED:           ['ANNULÉ'],
  EXPIRED:             ['EXPIRÉ'],
  BLOCKED:             ['BLOQUÉ'],
  NOT_FOUND:           ['NON', 'TROUVÉ'],
  SERVICE_UNAVAILABLE: ['HORS', 'LIGNE'],
};
