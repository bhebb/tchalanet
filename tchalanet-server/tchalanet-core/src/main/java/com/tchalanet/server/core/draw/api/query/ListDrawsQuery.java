package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import org.springframework.data.domain.Pageable;

public record ListDrawsQuery(DrawSearchCriteria criteria, Pageable pageable)
    implements Query<TchPage<DrawSummary>> {}
