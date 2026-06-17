package com.tchalanet.server.core.pricing.internal.application.port.out;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;

import java.util.List;
import java.util.Optional;

public interface SellerTerminalOddsOverrideReaderPort {

    Optional<SellerTerminalOddsOverride> findById(SellerTerminalOddsOverrideId id);

    List<SellerTerminalOddsOverride> findActiveBySellerTerminal(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId
    );

    Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        String betType,
        Short betOption
    );
}
