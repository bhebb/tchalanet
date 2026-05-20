package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;
import com.tchalanet.server.core.sales.api.query.VerifyTicketByPublicCodeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TicketVerifyService {

    private final QueryBus queryBus;

    public TicketVerificationView verify(String publicCode, String verificationCode) {
        var normalized = normalizePublicCode(publicCode);
        if (normalized.isBlank()) {
            throw ProblemRest.badRequest("ticket.invalid_code");
        }
        return queryBus.ask(new VerifyTicketByPublicCodeQuery(normalized, verificationCode));
    }

    private String normalizePublicCode(String code) {
        if (code == null) return "";
        return code.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
    }
}
