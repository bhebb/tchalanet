package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;

public record GetPublicTicketVerificationRecordQuery(String publicCode)
    implements Query<PublicTicketVerificationRecord> {}
