package com.tchalanet.server.features.stats.application;

import com.tchalanet.server.features.stats.domain.model.DrawStats;
import com.tchalanet.server.features.stats.domain.ports.in.GetDrawStatsQuery;
import com.tchalanet.server.features.stats.infra.persistence.mapper.StatsMapper;
import com.tchalanet.server.features.stats.infra.persistence.repository.SpringDrawStatsJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDrawStatsService implements GetDrawStatsQuery {

  private final SpringDrawStatsJpaRepository drawStatsJpaRepository;
  private final StatsMapper mapper;

  @Override
  public Optional<DrawStats> getStatsForDraw(UUID drawId) {
    return drawStatsJpaRepository.findById(drawId).map(mapper::toDomain);
  }
}
