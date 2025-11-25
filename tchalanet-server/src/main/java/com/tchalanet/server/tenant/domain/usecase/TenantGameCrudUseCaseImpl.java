package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.tenant.domain.model.TenantGame;
import com.tchalanet.server.tenant.domain.ports.TenantGameRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implémentation basique du use case CRUD pour les jeux d'un tenant. Pour l'instant, délègue
 * directement au repository de domaine.
 */
@Component
@RequiredArgsConstructor
public class TenantGameCrudUseCaseImpl implements TenantGameCrudUseCase {

  private final TenantGameRepository tenantGameRepository;

  @Override
  public TenantGame create(TenantGame t) {
    // TODO: ajouter des règles métier (validation, droits, etc.) si nécessaire
    return tenantGameRepository.save(t);
  }

  @Override
  public List<TenantGame> listByTenant(UUID tenantId) {
    return tenantGameRepository.findByTenant(TenantId.of(tenantId));
  }
}
