package com.tchalanet.server.features.publicdraw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultItemResponse;
import java.time.LocalDate;

public record GetPublicDrawResultQuery(String slotKey, LocalDate drawDate)
    implements Query<PublicDrawResultItemResponse> {}
