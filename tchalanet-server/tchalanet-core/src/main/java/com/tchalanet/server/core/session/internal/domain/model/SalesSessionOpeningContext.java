package com.tchalanet.server.core.session.internal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.Optional;

/**
 * Read-only fact snapshot for session opening eligibility.
 *
 * <p>Loaded in a single SQL query by {@code SalesSessionOpeningContextReaderPort}.
 * Evaluated by {@code SalesSessionOpeningEligibilityPolicy}.
 * Each boolean field represents a verifiable fact about the operational context.
 */
public record SalesSessionOpeningContext(
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId openedBy,

    /* tenant */
    boolean tenantExists,
    boolean tenantActive,

    /* app user */
    boolean userExists,
    boolean userActive,

    /* seller — the POS identity of the user within the tenant */
    boolean sellerExistsInTenant,
    boolean sellerActiveInTenant,
    boolean sellerCanOpenPosSession,

    /* outlet */
    boolean outletExists,
    boolean outletBelongsToTenant,
    boolean outletActive,
    boolean outletBlocked,

    /* terminal */
    boolean terminalExists,
    boolean terminalBelongsToTenant,
    boolean terminalBelongsToOutlet,
    boolean terminalActive,
    boolean terminalBlocked,
    boolean terminalBound,

    /* assignments */
    boolean sellerAllowedForOutlet,
    boolean sellerAllowedForTerminal,

    /* business calendar */
    boolean businessDayOpen,

    /* current session */
    Optional<SalesSessionId> currentOpenSessionId
) {}
