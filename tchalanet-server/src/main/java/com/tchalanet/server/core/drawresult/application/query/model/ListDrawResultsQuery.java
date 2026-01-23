package com.tchalanet.server.catalog.drawresult.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawResult;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record ListDrawResultsQuery(
    String provider, String slotKey, LocalDate from, LocalDate to, Pageable pageable)
    implements Query<TchPage<DrawResult>> {}
