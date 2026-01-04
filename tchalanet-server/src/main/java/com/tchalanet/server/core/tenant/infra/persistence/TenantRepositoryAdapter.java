package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenant.application.port.out.TenantDirectory;
import com.tchalanet.server.core.tenant.application.port.out.TenantOutletCheckPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.tenant.domain.model.Tenant;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantRepositoryAdapter
    implements TenantDirectory, TenantReaderPort, TenantWriterPort, TenantOutletCheckPort {

  private final TenantJpaRepository repo;

  // -------------------------
  // TenantOutletCheckPort
  // -------------------------
  @Override
  public boolean hasActiveOutlets(TenantId tenantId) {
    // TODO (quand core.outlet existe)
    // return outletRepo.existsByTenantIdAndStatusActiveAndDeletedAtIsNull(tenantId);
    return false;
  }

  // -------------------------
  // TenantReaderPort
  // -------------------------
  @Override
  public Optional<Tenant> findById(TenantId id) {
    return repo.findById(id.uuid())
        .filter(e -> e.getDeletedAt() == null)
        .map(TenantMapper::toDomain);
  }

  @Override
  public Optional<Tenant> findByCode(String codeLower) {
    return repo.findByCodeIgnoreCaseAndDeletedAtIsNull(codeLower).map(TenantMapper::toDomain);
  }

  @Override
  public boolean existsByCode(String codeLower) {
    return findByCode(codeLower).isPresent();
  }

  // -------------------------
  // TenantWriterPort
  // -------------------------
  @Override
  public Tenant save(Tenant tenant) {
    TenantJpaEntity entity =
        repo.findById(tenant.id().value())
            .map(e -> TenantMapper.merge(e, tenant))
            .orElseGet(() -> TenantMapper.toNewEntity(tenant));

    TenantJpaEntity saved = repo.save(entity);
    return TenantMapper.toDomain(saved);
  }

  @Override
  public void setActiveThemeId(TenantId tenantId, UUID themeId) {
    repo.updateActiveThemeId(tenantId.uuid(), themeId);
  }

  // -------------------------
  // TenantDirectory
  // -------------------------
  @Override
  public TenantId requireTenantIdByCode(String tenantCode) {
    if (tenantCode == null || tenantCode.isBlank()) {
      throw new IllegalArgumentException("tenantCode is required");
    }
    return repo.findByCodeIgnoreCaseAndDeletedAtIsNull(tenantCode)
        .map(TenantJpaEntity::getId)
        .map(TenantId::of)
        .orElseThrow(
            () -> new EntityNotFoundException("Tenant cannot be found with code: " + tenantCode));
  }

  @Override
  public boolean isTenantActive(TenantId tenantId) {
    if (tenantId == null) return false;
    return repo.findById(tenantId.uuid())
        .filter(e -> e.getDeletedAt() == null)
        .map(e -> e.getStatus() == TenantStatus.ACTIVE)
        .orElse(false);
  }

  @Override
  public List<TenantId> listActiveTenantIds() {
    return repo.findAll().stream()
        .filter(e -> e.getDeletedAt() == null && e.getStatus() == TenantStatus.ACTIVE)
        .map(com.tchalanet.server.core.tenant.infra.persistence.TenantJpaEntity::getId)
        .map(TenantId::of)
        .toList();
  }
}
