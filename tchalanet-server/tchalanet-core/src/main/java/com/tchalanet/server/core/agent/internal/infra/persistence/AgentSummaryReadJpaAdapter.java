package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.agent.api.query.model.AgentSummaryView;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentSummaryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AgentSummaryReadJpaAdapter implements AgentSummaryReadPort {
  private final AgentProjectionRepository repository;

  @Override
  public TchPage<AgentSummaryView> findSummaries(Pageable pageable) {
    return TchPageMapper.map(repository.findSummaries(pageable), p ->
        new AgentSummaryView(
            com.tchalanet.server.common.types.id.AgentId.of(p.id()),
            p.displayName(),
            p.type(),
            p.status(),
            p.createdAt()
        )
    );
  }
}

