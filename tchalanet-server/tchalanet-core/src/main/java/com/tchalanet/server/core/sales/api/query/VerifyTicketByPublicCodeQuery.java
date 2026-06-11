package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;

public record VerifyTicketByPublicCodeQuery(
    String publicCode
) implements Query<TicketVerificationView> {}
