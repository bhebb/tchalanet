package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.SettleableDrawIdsJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettleableDrawIdsJpaAdapter implements FindSettleableDrawIdsPort {

  private final SettleableDrawIdsJpaRepository repo;

  @Override
  public List<UUID> findSettleableDrawIds(SettleableDrawCriteria criteria) {
    return repo.findSettleableDrawIds(
        criteria.tenantId(),
        criteria.source(),
        criteria.provider(),
        criteria.channelCode(),
        criteria.from(),
        criteria.to(),
        criteria.maxDraws(),
        criteria.force());
  }
}

