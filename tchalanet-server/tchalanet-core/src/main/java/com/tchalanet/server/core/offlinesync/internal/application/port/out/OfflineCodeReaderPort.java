package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;

import java.util.Optional;

public interface OfflineCodeReaderPort {

    Optional<OfflineCode> findById(OfflineCodeId id);

    /** {@link #findById(OfflineCodeId)} variant that 404s when missing. */
    OfflineCode getRequired(OfflineCodeId id);

    /** Lookup a code by its short user-visible value (e.g. {@code "ABC123"}). */
    Optional<OfflineCode> findByCode(TenantId tenantId, OfflineGrantId grantId, String code);
}
