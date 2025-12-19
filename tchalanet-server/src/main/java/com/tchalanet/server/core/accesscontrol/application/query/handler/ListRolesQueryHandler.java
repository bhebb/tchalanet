package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.core.accesscontrol.application.query.model.ListRolesQuery;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListRolesQueryHandler implements QueryHandler<ListRolesQuery, List<TchRole>> {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public List<TchRole> handle(ListRolesQuery query) {
    UUID tenantId = query.tenantId();

    // Pour l'instant, on lit les rôles globaux + tenant depuis la table app_role et on
    // les mappe vers TchRole si applicable (rôles système). Les autres rôles peuvent être
    // ignorés ou retournés via un autre read model si besoin.
    List<AppRoleEntity> entities =
        tenantId == null
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(tenantId);

    return entities.stream()
        .filter(AppRoleEntity::isSystem)
        .map(e -> TchRole.valueOf(e.getCode()))
        .distinct()
        .toList();
  }
}
