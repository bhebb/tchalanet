package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;
import com.tchalanet.server.core.sales.api.query.VerifyTicketByPublicCodeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketVerifyService {

    private final QueryBus queryBus;

    public TicketVerificationView verify(String publicCode) {
        return queryBus.ask(new VerifyTicketByPublicCodeQuery(publicCode));
    }
}
