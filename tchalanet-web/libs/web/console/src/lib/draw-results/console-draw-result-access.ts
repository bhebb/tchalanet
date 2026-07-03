import { AuthSessionService } from '@tch/core/auth';

export type ConsoleDrawResultCapability = 'manual' | 'confirm' | 'override' | 'fetch';

const CAPABILITY_PERMISSIONS: Record<ConsoleDrawResultCapability, readonly string[]> = {
  manual: ['draw-results.manual', 'admin.draw-results.manual', 'platform.ops.draw-results.manual'],
  confirm: ['draw-results.confirm', 'platform.ops.draw-results.confirm'],
  override: ['draw-results.override', 'platform.ops.draw-results.override'],
  fetch: ['draw-results.fetch', 'platform.ops.draw-results.fetch'],
};

export function canUseDrawResultCapability(
  auth: AuthSessionService,
  capability: ConsoleDrawResultCapability,
): boolean {
  if (auth.hasRole('SUPER_ADMIN')) return true;
  return CAPABILITY_PERMISSIONS[capability].some(permission => auth.hasPermission(permission));
}
