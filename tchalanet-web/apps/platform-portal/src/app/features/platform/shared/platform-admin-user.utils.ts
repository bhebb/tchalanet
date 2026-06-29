import type { BadgeStatus } from '@tch/ui/components';

export function adminUserStatusBadge(status: string): BadgeStatus {
  const map: Record<string, BadgeStatus> = {
    ACTIVE: 'ready',
    PENDING: 'pending',
    SUSPENDED: 'warning',
    INACTIVE: 'missing',
    ARCHIVED: 'missing',
  };
  return map[status] ?? 'missing';
}
