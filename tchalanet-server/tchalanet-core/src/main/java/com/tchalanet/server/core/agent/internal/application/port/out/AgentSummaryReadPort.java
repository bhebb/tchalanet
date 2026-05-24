package com.tchalanet.server.core.agent.internal.application.port.out;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.agent.api.query.model.AgentSummaryView;
import org.springframework.data.domain.Pageable;

public interface AgentSummaryReadPort {
  TchPage<AgentSummaryView> findSummaries(Pageable pageable);
}

