import { AdminStatusTone } from '@tch/ui/console';

import {
  ConsoleDrawLifecycleDisplayAction,
  ConsoleDrawPublicationStatus,
  ConsoleDrawResultQuality,
  ConsoleDrawResultStatus,
  ConsoleDrawSalesStatus,
  ConsoleDrawStatus,
} from '../domain/console-domain-types';

export function consoleDrawStatusLabel(status: ConsoleDrawStatus | string | null | undefined): string {
  if (!status) return '—';
  switch (status) {
    case 'CANCELED': return 'domain.draw.status.CANCELLED';
    default: return `domain.draw.status.${status}`;
  }
}

export function consoleDrawStatusTone(status: ConsoleDrawStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'RESULTED':
    case 'RESULTS_APPLIED':
    case 'SETTLED': return 'info';
    case 'LOCKED':
    case 'PENDING_RESULTS':
    case 'SCHEDULED': return 'warning';
    case 'CANCELED':
    case 'CANCELLED': return 'danger';
    case 'ARCHIVED': return 'neutral';
    default: return 'neutral';
  }
}

export function consoleDrawSalesStatusLabel(status: ConsoleDrawSalesStatus | string | null | undefined): string {
  if (!status) return '—';
  return `domain.draw.salesStatus.${status}`;
}

export function consoleDrawSalesStatusTone(status: ConsoleDrawSalesStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'LOCKED': return 'warning';
    case 'CANCELLED': return 'danger';
    default: return 'neutral';
  }
}

export function consoleDrawResultStatusLabel(status: ConsoleDrawResultStatus | string | null | undefined): string {
  if (!status) return '—';
  return `domain.draw.resultStatus.${status}`;
}

export function consoleDrawResultStatusTone(status: ConsoleDrawResultStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'CONFIRMED':
    case 'APPLIED': return 'success';
    case 'PROVISIONAL':
    case 'CORRECTED':
    case 'OVERRIDDEN': return 'warning';
    case 'ERROR':
    case 'VOIDED':
    case 'MISSING':
    case 'SOURCE_ERROR':
    case 'REJECTED': return 'danger';
    case 'MANUAL': return 'info';
    default: return 'neutral';
  }
}

export function consoleDrawResultQualityLabel(quality: ConsoleDrawResultQuality | string | null | undefined): string {
  if (!quality) return '—';
  return `domain.draw.resultQuality.${quality}`;
}

export function consoleDrawResultQualityTone(quality: ConsoleDrawResultQuality | string | null | undefined): AdminStatusTone {
  switch (quality) {
    case 'COMPLETE':
    case 'OFFICIAL': return 'success';
    case 'SUSPECT':
    case 'MANUAL':
    case 'ESTIMATED': return 'warning';
    case 'INVALID': return 'danger';
    default: return 'neutral';
  }
}

export function consoleDrawPublicationStatusLabel(status: ConsoleDrawPublicationStatus | string | null | undefined): string {
  if (!status || status === 'NOT_PUBLISHED') return 'domain.draw.publicationStatus.NOT_PUBLISHED';
  return `domain.draw.publicationStatus.${status}`;
}

export function consoleDrawPublicationStatusTone(status: ConsoleDrawPublicationStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'PUBLISHED': return 'success';
    case 'PROVISIONAL': return 'warning';
    default: return 'neutral';
  }
}

export function consoleDrawLifecycleActionLabel(action: ConsoleDrawLifecycleDisplayAction): string {
  return `domain.draw.lifecycleAction.${action}`;
}

export function consoleDrawLifecycleActionIcon(action: ConsoleDrawLifecycleDisplayAction): string {
  switch (action) {
    case 'open': return 'play_arrow';
    case 'close': return 'pause_circle';
    case 'lock': return 'lock';
    case 'unlock': return 'lock_open';
    case 'cancel': return 'cancel';
    case 'settle': return 'paid';
    case 'archive': return 'inventory_2';
    case 'reschedule': return 'schedule';
    case 'correct': return 'edit';
  }
}
