package com.tchalanet.server.features.publicdraw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultListResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record ListPublicDrawResultsQuery(
    String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable)
    implements Query<PublicDrawResultListResponse> {}
