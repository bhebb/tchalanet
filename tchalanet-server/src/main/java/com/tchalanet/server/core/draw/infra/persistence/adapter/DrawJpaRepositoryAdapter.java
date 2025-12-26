package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DrawJpaRepositoryAdapter implements DrawReaderPort, DrawWriterPort {

  private final DrawJpaRepository jpa;
  private final DrawMapper mapper;

  @Override
  public Optional<Draw> findById( DrawId drawId) {
    return jpa.findById(drawId.uuid()).map(mapper::toDomain);
  }

  @Override
  public List<Draw> findClosableDraws( TenantId tenantId, ZonedDateTime now) {
    Instant inst = now.toInstant();
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse("CLOSED", inst)
        .stream()
        .filter(e -> tenantId == null || tenantId.uuid().equals(e.getTenantId()))
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Draw> findResultedUnsettled( TenantId tenantId, ZonedDateTime now) {
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse(
            "RESULTED", now.toInstant())
        .stream()
        .filter(e -> tenantId == null || tenantId.uuid().equals(e.getTenantId()))
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Draw> findNext(GetNextDrawQuery query) {
    // Not yet implemented
    return Optional.empty();
  }

  @Override
  public List<DrawSummary> findByCriteria(DrawSearchCriteria drawSearchCriteria) {
    return List.of();
  }

  @Override
  public List<Draw> findNextForChannels(GetNextDrawsQuery query) {
    return List.of();
  }

  @Override
  public Draw save(Draw draw) {
    var entity = mapper.toEntity(draw);
    var saved = jpa.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Draw> saveAll(List<Draw> draws) {
    var entities = draws.stream().map(mapper::toEntity).collect(Collectors.toList());
    var saved = jpa.saveAll(entities);
    return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<Draw> updateDraws(List<Draw> draws) {
    var entities = draws.stream().map(mapper::toEntity).collect(Collectors.toList());
    var saved = jpa.saveAll(entities);
    return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
  }
}
