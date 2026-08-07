import { AdminStatusTone } from '@tch/ui/console';

import {
  TicketResultStatus,
  TicketSettlementStatus,
  TicketStatus,
} from '../../features/sales-admin/data-access/admin-tickets-api.service';

export const TICKET_STATUS_VALUES: readonly TicketStatus[] = [
  'PENDING_APPROVAL',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
  'VOIDED',
];

export function isTicketStatus(value: string | null): value is TicketStatus {
  return TICKET_STATUS_VALUES.includes(value as TicketStatus);
}

export function ticketStatusLabelKey(status: TicketStatus | string | null | undefined): string {
  switch (status) {
    case 'PENDING_APPROVAL':
      return 'admin.tickets.status.pendingApproval';
    case 'APPROVED':
      return 'admin.tickets.status.approved';
    case 'REJECTED':
      return 'admin.tickets.status.rejected';
    case 'CANCELLED':
      return 'admin.tickets.status.cancelled';
    case 'VOIDED':
      return 'admin.tickets.status.voided';
    default:
      return 'admin.tickets.status.unknown';
  }
}

export function ticketStatusTone(
  status: TicketStatus | string | null | undefined,
): AdminStatusTone {
  switch (status) {
    case 'PENDING_APPROVAL':
      return 'warning';
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
    case 'CANCELLED':
    case 'VOIDED':
      return 'danger';
    default:
      return 'neutral';
  }
}

export const TICKET_RESULT_STATUS_VALUES: readonly TicketResultStatus[] = [
  'NOT_RESULTED',
  'PENDING',
  'WON',
  'LOST',
  'VOID',
  'OVERRIDDEN',
];

export const TICKET_SETTLEMENT_STATUS_VALUES: readonly TicketSettlementStatus[] = [
  'NOT_SETTLED',
  'PAYOUT_PENDING',
  'SETTLED',
  'NO_PAYOUT',
  'PAID',
  'REVERSED',
];

export function isTicketResultStatus(value: string | null): value is TicketResultStatus {
  return TICKET_RESULT_STATUS_VALUES.includes(value as TicketResultStatus);
}

export function ticketResultStatusLabelKey(
  status: TicketResultStatus | string | null | undefined,
): string {
  switch (status) {
    case 'NOT_RESULTED':
      return 'admin.tickets.resultStatus.notResulted';
    case 'PENDING':
      return 'admin.tickets.resultStatus.pending';
    case 'WON':
      return 'admin.tickets.resultStatus.won';
    case 'LOST':
      return 'admin.tickets.resultStatus.lost';
    case 'VOID':
      return 'admin.tickets.resultStatus.void';
    case 'OVERRIDDEN':
      return 'admin.tickets.resultStatus.overridden';
    default:
      return 'admin.tickets.resultStatus.unknown';
  }
}

export function ticketResultStatusTone(
  status: TicketResultStatus | string | null | undefined,
): AdminStatusTone {
  switch (status) {
    case 'WON':
      return 'success';
    case 'LOST':
    case 'VOID':
      return 'danger';
    case 'PENDING':
    case 'NOT_RESULTED':
      return 'warning';
    case 'OVERRIDDEN':
      return 'info';
    default:
      return 'neutral';
  }
}

export function ticketSettlementStatusLabelKey(
  status: TicketSettlementStatus | string | null | undefined,
): string {
  switch (status) {
    case 'NOT_SETTLED':
      return 'admin.tickets.settlementStatus.notSettled';
    case 'PAYOUT_PENDING':
      return 'admin.tickets.settlementStatus.payoutPending';
    case 'SETTLED':
      return 'admin.tickets.settlementStatus.settled';
    case 'NO_PAYOUT':
      return 'admin.tickets.settlementStatus.noPayout';
    case 'PAID':
      return 'admin.tickets.settlementStatus.paid';
    case 'REVERSED':
      return 'admin.tickets.settlementStatus.reversed';
    default:
      return 'admin.tickets.settlementStatus.unknown';
  }
}
