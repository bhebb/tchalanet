package com.tchalanet.server.features.pagemodel.security;

import com.tchalanet.server.common.security.TchRole;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Server-side access policy for private PageModel documents.
 *
 * <p>Maps each private logicalId to the set of roles that may access it. If a logicalId has no
 * entry (public or unknown), access is not restricted here — authorization happens upstream at the
 * Spring Security level.
 *
 * <p>[harden-pagemodel-security-v2 / D2] The client never chooses which logicalId to load.
 * Controllers resolve logicalId from TchRequestContext. This policy is a server-side guard that
 * prevents internal code paths from resolving a private PageModel for the wrong role.
 */
@Component
public class PageModelAccessPolicy {

  private static final Map<String, Set<TchRole>> POLICIES =
      Map.of(
          "private.dashboard.cashier.web", Set.of(TchRole.CASHIER),
          "private.dashboard.tenant_admin", Set.of(TchRole.TENANT_ADMIN),
          "private.dashboard.superadmin", Set.of(TchRole.SUPER_ADMIN));

  /**
   * Returns true if {@code role} is allowed to access the PageModel identified by {@code
   * logicalId}. Public pages (no entry in the policy map) always return true.
   */
  public boolean permits(String logicalId, TchRole role) {
    Set<TchRole> allowed = POLICIES.get(logicalId);
    if (allowed == null) {
      return true; // public or unknown — not restricted by this policy
    }
    return role != null && allowed.contains(role);
  }
}
