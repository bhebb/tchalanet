package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;

import java.util.Optional;
import java.util.UUID;

public interface OfflineGrantReaderPort {

    Optional<OfflineGrant> findById(OfflineGrantId id);

    /** {@link #findById(OfflineGrantId)} variant that 404s when missing. */
    OfflineGrant getRequired(OfflineGrantId id);

    /**
     * Most recent {@code ACTIVE} grant for the given seller/terminal/device combination,
     * or empty if none. Used by {@code GetCurrentOfflineGrantQuery} and by the sync handler
     * to resolve which grant a submission belongs to.
     */
    Optional<OfflineGrant> findCurrentActive(UserId sellerUserId, TerminalId terminalId, UUID deviceId);
}
