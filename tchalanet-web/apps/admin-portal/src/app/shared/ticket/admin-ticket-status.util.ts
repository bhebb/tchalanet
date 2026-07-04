import { AdminStatusTone } from '@tch/ui/console';

import { TicketStatus } from '../../features/sales-admin/data-access/admin-tickets-api.service';

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

export function ticketStatusTone(status: TicketStatus | string | null | undefined): AdminStatusTone {
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
