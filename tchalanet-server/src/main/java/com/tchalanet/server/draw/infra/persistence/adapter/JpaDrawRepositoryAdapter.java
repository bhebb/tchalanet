package com.tchalanet.server.draw.infra.persistence.adapter;

import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.infra.persistence.entity.DrawEntity;
import com.tchalanet.server.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.draw.infra.persistence.repository.SpringDrawJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaDrawRepositoryAdapter implements DrawRepository {

  private final SpringDrawJpaRepository jpaRepository;
  private final DrawMapper mapper;

  @Override
  public Optional<Draw> findById(UUID id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Draw save(Draw draw) {
    DrawEntity entity = mapper.toEntity(draw);
    DrawEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public List<Draw> findByTenantAndScheduledAtBetween(UUID tenantId, Instant from, Instant to) {
    return jpaRepository.findByTenantIdAndScheduledAtBetween(tenantId, from, to).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public boolean existsByTenantChannelAndScheduledAt(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt) {
    return jpaRepository.existsByTenantIdAndDrawChannelIdAndScheduledAt(
        tenantId, drawChannelId, scheduledAt);
  }

  @Override
  public boolean saveIfNotExists(Draw draw) {
    // This method would typically involve a custom query or a check-then-insert logic
    // For simplicity, we'll just save and rely on unique constraints if they exist.
    // A more robust solution might use ON CONFLICT DO NOTHING in SQL.
    if (existsByTenantChannelAndScheduledAt(
        draw.tenantId(), draw.drawChannelId(), draw.scheduledAt())) {
      return false;
    }
    save(draw);
    return true;
  }

  @Override
  public List<Draw> findByStatusAndScheduledAtBefore(String status, Instant before) {
    // Assuming DrawStatus enum is used in entity
    return jpaRepository
        .findByStatusAndScheduledAtBefore(DrawStatus.valueOf(status), before)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Draw> findScheduledDrawsPastCutoff(UUID tenantId, Instant now) {
    return jpaRepository.findScheduledDrawsPastCutoff(tenantId, now).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
