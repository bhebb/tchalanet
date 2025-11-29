package com.tchalanet.server.draw.infra.persistence.adapter;

import com.tchalanet.server.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.infra.persistence.DrawJpaRepository;
import com.tchalanet.server.draw.infra.persistence.mapper.DrawMapper;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawJpaRepositoryAdapter implements DrawReaderPort, DrawWriterPort {

  private final DrawJpaRepository jpa;
  private final DrawMapper mapper;

  @Override
  public Optional<Draw> findById(UUID tenantId, UUID drawId) {
    return jpa.findById(drawId).map(mapper::toDomain);
  }

  @Override
  public List<Draw> findClosableDraws(UUID tenantId, ZonedDateTime now) {
    Instant inst = now.toInstant();
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse("CLOSED", inst)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Draw> findResultedUnsettled(UUID tenantId, ZonedDateTime now) {
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse(
            "RESULTED", now.toInstant())
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Draw> findNext(GetNextDrawQuery query) {
    // Not yet implemented
    return Optional.empty();
  }

  @Override
  public List<com.tchalanet.server.draw.domain.model.DrawSummary> findByCriteria(
      com.tchalanet.server.draw.application.query.model.DrawSearchCriteria drawSearchCriteria) {
    return List.of();
  }

  @Override
  public List<Draw> findNextForChannels(
      com.tchalanet.server.draw.application.query.model.GetNextDrawsQuery query) {
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
