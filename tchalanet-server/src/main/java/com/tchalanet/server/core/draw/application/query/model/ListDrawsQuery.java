package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public record ListDrawsQuery(DrawSearchCriteria criteria, Pageable pageable)
    implements Query<TchPage<DrawSummary>> {}
