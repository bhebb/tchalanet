import { Pipe, PipeTransform, inject } from '@angular/core';

import { AccessRequirement, AccessService } from './access.service';

/**
 * Pipe form of the access gate, for binding contexts where a structural directive doesn't fit —
 * e.g. `[disabled]`, `[attr.aria-hidden]`, or an `@if` condition.
 *
 *   <button [disabled]="!({ feature: 'web.payouts', entitlement: 'payouts' } | can)">…</button>
 *   @if ({ feature: 'web.x' } | can) { … }
 *
 * Impure so it re-evaluates as flags/entitlements resolve (the decision reads signals). The check is
 * a cheap set/flag lookup. For showing/hiding a whole element prefer the `*tchCan` directive.
 */
@Pipe({ name: 'can', pure: false })
export class CanPipe implements PipeTransform {
  private readonly access = inject(AccessService);

  transform(requirement: AccessRequirement): boolean {
    return this.access.can(requirement);
  }
}
