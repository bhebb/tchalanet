package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;

public record GetPublicTicketVerificationRecordQuery(String publicCode)
    implements Query<PublicTicketVerificationRecord> {}
