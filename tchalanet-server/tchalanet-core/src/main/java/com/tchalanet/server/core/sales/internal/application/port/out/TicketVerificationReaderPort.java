package com.tchalanet.server.core.sales.internal.application.port.out;

import java.util.Optional;

public interface TicketVerificationReaderPort {
    Optional<TicketVerificationProjection> findByPublicCodeAndVerificationCode(
        String publicCode,
        String verificationCode
    );
}
