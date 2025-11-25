package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.TenantUser;
import com.tchalanet.server.tenant.domain.ports.TenantUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TenantUserRepositoryAdapter implements TenantUserRepository {

  private final TenantUserJpaRepository repo;

  public TenantUserRepositoryAdapter(TenantUserJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public TenantUser save(TenantUser t) {
    TenantUserJpaEntity e = mapToEntity(t);
    TenantUserJpaEntity saved = repo.save(e);
    return mapToDomain(saved);
  }

  @Override
  public List<TenantUser> findByTenantId(UUID tenantId) {
    return repo.findByTenantId(tenantId).stream()
        .map(this::mapToDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<TenantUser> findByUserId(UUID userId) {
    return repo.findByUserId(userId).stream().map(this::mapToDomain).collect(Collectors.toList());
  }

  @Override
  public Optional<TenantUser> findByTenantIdAndUserId(UUID tenantId, UUID userId) {
    return repo.findByTenantIdAndUserId(tenantId, userId).map(this::mapToDomain);
  }

  @Override
  public void deleteByTenantIdAndUserId(UUID tenantId, UUID userId) {
    repo.deleteByTenantIdAndUserId(tenantId, userId);
  }

  // mappers (simple)
  private TenantUser mapToDomain(TenantUserJpaEntity e) {
    return new TenantUser(
        e.getId(), e.getTenantId(), e.getUserId(), e.getRole(), e.getAutonomyLevel(), e.isOwner());
  }

  private TenantUserJpaEntity mapToEntity(TenantUser d) {
    TenantUserJpaEntity e = new TenantUserJpaEntity();
    e.setId(d.id());
    e.setTenantId(d.tenantId());
    e.setUserId(d.userId());
    e.setRole(d.role());
    e.setAutonomyLevel(d.autonomyLevel());
    e.setOwner(d.owner());
    return e;
  }
}
