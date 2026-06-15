package com.tchalanet.server.common.context;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.Set;

/**
 * Temporary HTTP pipeline input produced by AccessResolutionFilter.
 *
 * <p>Invariants:
 * <ul>
 *   <li>APP_USER: appUserId required, sellerTerminalId null.</li>
 *   <li>SELLER_TERMINAL: sellerTerminalId required, effectiveTenantId required, appUserId null,
 *       roleCodes empty, permissionKeys limited to terminal actions.</li>
 *   <li>SYSTEM: not produced by the HTTP access-context pipeline.</li>
 * </ul>
 *
 * <p>After {@code TchContextFilter} runs, application code reads only {@code TchRequestContext}.
 * Stored as request attribute {@link TchContextRequestAttributes#RESOLVED_ACCESS}.
 */
public record ResolvedAccessContext(
    TchActorType actorType,
    UserId appUserId,
    SellerTerminalId sellerTerminalId,
    TenantId effectiveTenantId,
    boolean superAdmin,
    boolean tenantOverride,
    Set<String> roleCodes,
    Set<String> permissionKeys
) {
    public ResolvedAccessContext {
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
        permissionKeys = permissionKeys == null ? Set.of() : Set.copyOf(permissionKeys);
    }

    public boolean isAppUser() {
        return actorType == TchActorType.APP_USER;
    }

    public boolean isSellerTerminal() {
        return actorType == TchActorType.SELLER_TERMINAL;
    }

    public boolean isSystem() {
        return actorType == TchActorType.SYSTEM;
    }
}
