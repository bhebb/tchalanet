package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.TenantGame;
import com.tchalanet.server.tenant.domain.model.TenantId;
import com.tchalanet.server.tenant.domain.ports.TenantGameRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantGameRepositoryAdapter implements TenantGameRepository {

  private final TenantGameJpaRepository repo;

  @Override
  public TenantGame save(TenantGame t) {
    var e = TenantGameMapper.toEntity(t);
    var saved = repo.save(e);
    return TenantGameMapper.toDomain(saved);
  }

  @Override
  public List<TenantGame> findByTenant(TenantId tenantId) {
    return repo.findByTenantId(tenantId.getId()).stream()
        .map(TenantGameMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(java.util.UUID id) {
    repo.deleteById(id);
  }
}
