import type { VerificationStatus } from '@tch/page-model';

import type { VerificationCopy } from './public-check-ticket.model';

export { extractPublicCodeFromQr, normalizePublicCode } from './public-ticket.service';

export function formatPublicCode(value: string): string {
  const compact = value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 8);
  if (compact.length <= 4) return compact;
  return `${compact.slice(0, 4)}-${compact.slice(4)}`;
}

export function verificationCopy(status: VerificationStatus): VerificationCopy {
  const map: Record<VerificationStatus, VerificationCopy> = {
    WINNING_PAYABLE: {
      icon: 'task_alt', tone: 'success',
      titleKey: 'public.check.status.WINNING_PAYABLE.title',
      bodyKey:  'public.check.status.WINNING_PAYABLE.body',
    },
    WINNING_PAID: {
      icon: 'check_circle', tone: 'neutral',
      titleKey: 'public.check.status.WINNING_PAID.title',
      bodyKey:  'public.check.status.WINNING_PAID.body',
    },
    LOST: {
      icon: 'remove_circle', tone: 'neutral',
      titleKey: 'public.check.status.LOST.title',
      bodyKey:  'public.check.status.LOST.body',
    },
    PENDING_RESULT: {
      icon: 'schedule', tone: 'warning',
      titleKey: 'public.check.status.PENDING_RESULT.title',
      bodyKey:  'public.check.status.PENDING_RESULT.body',
    },
    CANCELLED: {
      icon: 'block', tone: 'danger',
      titleKey: 'public.check.status.CANCELLED.title',
      bodyKey:  'public.check.status.CANCELLED.body',
    },
    EXPIRED: {
      icon: 'timer_off', tone: 'neutral',
      titleKey: 'public.check.status.EXPIRED.title',
      bodyKey:  'public.check.status.EXPIRED.body',
    },
    BLOCKED: {
      icon: 'lock', tone: 'danger',
      titleKey: 'public.check.status.BLOCKED.title',
      bodyKey:  'public.check.status.BLOCKED.body',
    },
    NOT_FOUND: {
      icon: 'search_off', tone: 'danger',
      titleKey: 'public.check.status.NOT_FOUND.title',
      bodyKey:  'public.check.status.NOT_FOUND.body',
    },
    SERVICE_UNAVAILABLE: {
      icon: 'cloud_off', tone: 'neutral',
      titleKey: 'public.check.status.SERVICE_UNAVAILABLE.title',
      bodyKey:  'public.check.status.SERVICE_UNAVAILABLE.body',
    },
  };
  return map[status];
}
