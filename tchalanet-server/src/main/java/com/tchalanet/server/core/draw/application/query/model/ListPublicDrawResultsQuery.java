package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultPageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record ListPublicDrawResultsQuery(
    String channelCode, LocalDate from, LocalDate to, Pageable pageable)
    implements Query<PublicDrawResultPageResponse> {}
