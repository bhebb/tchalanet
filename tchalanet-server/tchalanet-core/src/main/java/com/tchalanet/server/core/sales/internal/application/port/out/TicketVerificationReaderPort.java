package com.tchalanet.server.core.sales.internal.application.port.out;

import java.util.Optional;

public interface TicketVerificationReaderPort {
    Optional<TicketVerificationProjection> findByPublicCode(
        String publicCode
    );
}
