import { AccessRequirement } from '@tch/core/auth';

export type ConsoleDrawResultCapability = 'manual' | 'confirm' | 'override' | 'fetch';

const CAPABILITY_PERMISSIONS: Record<ConsoleDrawResultCapability, readonly string[]> = {
  manual: [
    'draw_result.record_manual',
    'draw-results.manual',
    'admin.draw-results.manual',
    'platform.ops.draw-results.manual',
  ],
  confirm: ['draw_result.confirm', 'draw-results.confirm', 'platform.ops.draw-results.confirm'],
  override: ['draw_result.override', 'draw-results.override', 'platform.ops.draw-results.override'],
  fetch: ['draw-results.fetch', 'platform.ops.draw-results.fetch'],
};

export const CONSOLE_DRAW_RESULT_ACCESS: Record<
  ConsoleDrawResultCapability,
  readonly AccessRequirement[]
> = {
  manual: [{ role: 'SUPER_ADMIN' }, { permission: CAPABILITY_PERMISSIONS.manual }],
  confirm: [{ role: 'SUPER_ADMIN' }, { permission: CAPABILITY_PERMISSIONS.confirm }],
  override: [{ role: 'SUPER_ADMIN' }, { permission: CAPABILITY_PERMISSIONS.override }],
  fetch: [{ role: 'SUPER_ADMIN' }, { permission: CAPABILITY_PERMISSIONS.fetch }],
};
