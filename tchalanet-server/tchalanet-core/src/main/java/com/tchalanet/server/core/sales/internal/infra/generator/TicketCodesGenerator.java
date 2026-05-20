package com.tchalanet.server.core.sales.internal.infra.generator;

import com.tchalanet.server.core.sales.api.model.value.PublicCode;
import com.tchalanet.server.core.sales.api.model.value.TicketCode;
import com.tchalanet.server.core.sales.api.model.value.VerificationCode;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketCodeGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketCodesGenerator implements TicketCodeGeneratorPort {

    private final CrockfordPublicCodeGenerator crockfordPublicCodeGenerator;
    private final TimeBasedTicketNumberGenerator timeBasedTicketNumberGenerator;
    private final VerificationCodeGenerator verificationCodeGenerator;

    @Override
    public TicketCode nextTicketCode() {
        return TicketCode.of(timeBasedTicketNumberGenerator.generate());
    }

    @Override
    public PublicCode nextPublicCode() {
        return PublicCode.of(crockfordPublicCodeGenerator.generate());
    }

    @Override
    public VerificationCode nextVerificationCode() {
        return VerificationCode.of(verificationCodeGenerator.generate());
    }
}
