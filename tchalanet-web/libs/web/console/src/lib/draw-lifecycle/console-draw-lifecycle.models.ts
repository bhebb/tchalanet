import { TchRequestOptions } from '@tch/api';

export type ConsoleDrawLifecycleAction =
  | 'open'
  | 'close'
  | 'cancel'
  | 'lock'
  | 'unlock'
  | 'settle'
  | 'archive';

export interface ConsoleDrawLifecycleRequest {
  readonly drawIds: readonly string[];
  readonly reason?: string | null;
  readonly reasonCode?: string | null;
  readonly reasonLabel?: string | null;
  readonly force?: boolean | null;
}

export type ConsoleDrawLifecycleOneRequest = Omit<ConsoleDrawLifecycleRequest, 'drawIds'>;

export interface ConsoleTenantAdminContext {
  readonly tenantId?: string | null;
  readonly reason: string;
}

export function consoleTenantAdminOptions(
  context: ConsoleTenantAdminContext | null | undefined,
  options?: TchRequestOptions,
): TchRequestOptions | undefined {
  if (!context?.tenantId) return options;
  return {
    ...(options ?? {}),
    asTenantAdmin: {
      tenantId: context.tenantId,
      reason: context.reason,
    },
  };
}
