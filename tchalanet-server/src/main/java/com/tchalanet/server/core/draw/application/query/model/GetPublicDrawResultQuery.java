package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import java.time.LocalDate;

public record GetPublicDrawResultQuery(String channelCode, LocalDate drawDate)
    implements Query<PublicDrawResultItemResponse> {}
