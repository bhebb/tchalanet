package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.draw.domain.model.TenantId;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelRepositoryAdapter implements DrawChannelRepository {

  private final DrawChannelJpaRepository repo;
  private final LogAuditEventUseCase audit;

  @Override
  public DrawChannel save(DrawChannel d) {
    var e = DrawChannelMapper.toEntity(d);
    var saved = repo.save(e);
    // audit
    try {
      var event =
          AuditEvent.of(
              saved.getTenantId(),
              AuditActorType.SYSTEM,
              "system",
              AuditEntityType.DRAW,
              saved.getId().toString(),
              AuditAction.UPDATE,
              null,
              null,
              null);
      audit.log(event);
    } catch (Exception ignored) {
    }
    return DrawChannelMapper.toDomain(saved);
  }

  @Override
  public Optional<DrawChannel> findById(DrawChannelId id) {
    return repo.findById(id == null ? null : id.value()).map(DrawChannelMapper::toDomain);
  }

  @Override
  public List<DrawChannel> findByTenant(
      com.tchalanet.server.tenant.domain.model.TenantId tenantId) {
    return List.of();
  }

  public List<DrawChannel> findByTenant(TenantId tenantId) {
    return repo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId.value()).stream()
        .map(DrawChannelMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<DrawChannel> findAllActive() {
    return repo.findByActiveTrueOrderBySortOrder().stream()
        .map(DrawChannelMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(DrawChannelId id) {
    try {
      var event =
          AuditEvent.of(
              id == null ? null : null,
              AuditActorType.SYSTEM,
              "system",
              AuditEntityType.DRAW,
              id == null ? null : id.value().toString(),
              AuditAction.DELETE,
              null,
              null,
              null);
      audit.log(event);
    } catch (Exception ignored) {
    }
    repo.deleteById(id.value());
  }
}
