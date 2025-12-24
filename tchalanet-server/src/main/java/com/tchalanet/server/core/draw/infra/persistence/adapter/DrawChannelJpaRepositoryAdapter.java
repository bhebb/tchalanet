package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.core.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.core.audit.domain.model.AuditAction;
import com.tchalanet.server.core.audit.domain.model.AuditEntityType;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.persistence.entity.DrawChannelJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawChannelMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelJpaRepositoryAdapter
    implements DrawChannelReaderPort, DrawChannelWriterPort {

  private final DrawChannelJpaRepository repo;
  private final LogAuditEventCommandHandler audit;
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

  @Override
  public List<DrawChannel> saveAll(List<DrawChannel> channels) {
    var entities = channels.stream().map(mapper::toEntity).collect(Collectors.toList());
    var saved = repo.saveAll(entities);
    return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  public Optional<DrawChannel> findById(DrawChannelId id) {
    if (id == null) return Optional.empty();
    return repo.findById(id.value()).map(mapper::toDomain);
  }

  // Port method: include tenantId parameter as required by DrawChannelReaderPort
  @Override
  public Optional<DrawChannel> findById(UUID tenantId, DrawChannelId id) {
    if (id == null) return Optional.empty();
    // tenantId is not used by the JPA lookup by id, but kept to satisfy the port signature
    return findById(id);
  }

  @Override
  public Optional<DrawChannel> findByCode(UUID tenantId, String code) {
    return repo.findByTenantIdAndCode(tenantId, code).map(mapper::toDomain);
  }

  @Override
  public List<DrawChannel> findActiveByTenant(UUID tenantId) {
    return repo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<DrawChannelSummary> findByCriteria(DrawChannelSearchCriteria criteria) {
    return List.of();
  }

  public List<DrawChannel> findByTenant(
      com.tchalanet.server.core.tenant.domain.model.TenantId tenantId) {
    if (tenantId == null) return List.of();
    return repo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId.value()).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  public List<DrawChannel> findAllActive() {
    return repo.findByActiveTrueOrderBySortOrder().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
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
}
