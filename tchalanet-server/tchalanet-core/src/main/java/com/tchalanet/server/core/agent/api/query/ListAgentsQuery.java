package com.tchalanet.server.core.agent.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.agent.api.query.model.AgentSummaryView;
import org.springframework.data.domain.Pageable;

public record ListAgentsQuery(Pageable pageable) implements Query<TchPage<AgentSummaryView>> {}

