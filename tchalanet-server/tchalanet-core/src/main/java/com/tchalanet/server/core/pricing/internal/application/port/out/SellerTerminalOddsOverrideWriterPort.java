package com.tchalanet.server.core.pricing.internal.application.port.out;

import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;

public interface SellerTerminalOddsOverrideWriterPort {

    SellerTerminalOddsOverride save(SellerTerminalOddsOverride override);

    void delete(SellerTerminalOddsOverride override);
}
