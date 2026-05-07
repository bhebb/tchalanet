package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;

/**
 * Cross-domain port. Lets {@code core.outlet} delegate the workplace assignment of a user (which
 * lives on {@code tenant_user.outlet_id}) to {@code core.tenantuser} without depending on it
 * directly.
 *
 * <p>Implementations must be idempotent and respect tenant scoping via the request context (RLS
 * enforced at DB level).
 */
public interface OutletMembershipPort {

  /**
   * Assigns the given user to the given outlet. If the user was previously assigned to another
   * outlet, the previous assignment is replaced. No-op if already assigned to the same outlet.
   *
   * @throws IllegalStateException if no membership exists for the user in the current tenant.
   */
  void assignUserToOutlet(OutletId outletId, UserId userId);

  /**
   * Removes the user from the given outlet (sets {@code tenant_user.outlet_id} to {@code null}).
   * No-op if the user is not assigned to this outlet.
   *
   * @throws IllegalStateException if no membership exists for the user in the current tenant.
   */
  void removeUserFromOutlet(OutletId outletId, UserId userId);
}
