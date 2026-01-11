package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.audit.application.command.handler.AuditLoggingCommandHandler;
import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawChannelMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelJpaRepositoryAdapter
    implements DrawChannelReaderPort, DrawChannelWriterPort {

  private final DrawChannelJpaRepository repo;
  private final AuditLoggingCommandHandler audit;
  private final DrawChannelMapper mapper;

  @Override
  public DrawChannel save(DrawChannel d) {
    var e = mapper.toEntity(d);
    var saved = repo.save(e);
    // audit
    try {
      audit.handle(
          new LogAuditEventCommand(
              AuditEntityType.DRAW, saved.getId().toString(), AuditAction.UPDATE, null));
    } catch (Exception ex) {
      // ignore audit failures
    }
    return mapper.toDomain(saved);
  }

  public Optional<DrawChannel> findById(DrawChannelId id) {
    if (id == null) return Optional.empty();
    return repo.findById(id.value()).map(mapper::toDomain);
  }

  // Port method: include tenantId parameter as required by DrawChannelReaderPort
  @Override
  public Optional<DrawChannel> findById(TenantId tenantId, DrawChannelId id) {
    if (id == null) return Optional.empty();
    // tenantId is not used by the JPA lookup by id, but kept to satisfy the port signature
    return findById(id);
  }

  @Override
  public Optional<DrawChannel> findByCode(TenantId tenantId, String code) {
    return repo.findByTenantIdAndCode(tenantId.uuid(), code).map(mapper::toDomain);
  }

  @Override
  public List<DrawChannelSummary> findByCriteria(DrawChannelSearchCriteria criteria) {
    if (criteria == null || criteria.tenantId() == null) return List.of();

    var tenantUuid = criteria.tenantId().uuid();
    List<com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity> entities;
    if (Boolean.TRUE.equals(criteria.activeOnly())) {
      entities = repo.findByTenantIdAndActiveTrueOrderBySortOrderAsc(tenantUuid);
    } else {
      entities = repo.findByTenantIdOrderBySortOrderAsc(tenantUuid);
    }

    return entities.stream()
        .map(
            e -> {
              String code = e.getCode();
              String name = e.getName() == null ? code : e.getName();
              boolean active = e.isActive();
              // We don't have scheduled draw instants here; leave times/status empty and lastResult
              // empty.
              return new DrawChannelSummary(code, name, null, null, null, false, active, List.of());
            })
        .toList();
  }

  public void deleteById(DrawChannelId id) {
    if (id == null) return;
    try {
      java.util.Map<String, Object> details = null;
      audit.handle(
          new LogAuditEventCommand(
              AuditEntityType.DRAW, id.value().toString(), AuditAction.DELETE, details));
    } catch (Exception ex) {
      // ignore audit failures
    }
    repo.deleteById(id.value());
  }

  @Override
  public List<DrawChannelCalendarRow> listActiveCalendarRows(TenantId tenantId) {
    return repo
        .findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(tenantId.uuid())
        .stream()
        .map(this::toRow)
        .toList();
  }

  private DrawChannelCalendarRow toRow(DrawChannelJpaEntity e) {
    return new DrawChannelCalendarRow(
        DrawChannelId.of(e.getId()),
        /* tenantGameId not available on DrawChannelJpaEntity in this schema */
        null,
        e.getCode(),
        e.getTimezone(),
        e.getDrawTime(),
        e.getCutoffSec(),
        e.getDaysOfWeek(),
        null,
        e.isActive(),
        e.isActive(),
        e.getSortOrder(),
        null);
  }
}
