package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.accesscontrol.application.port.out.RoleReaderPort;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleReaderJpaAdapter implements RoleReaderPort {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public List<TchRole> listSystemRolesForTenant(TenantId tenantId) {
    var entities =
        tenantId == null
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(tenantId.uuid());

    return entities.stream()
        .filter(r -> r.getTenantId() == null)
        .map(
            e -> {
              try {
                String code = e.getCode();
                return code == null
                    ? null
                    : TchRole.valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
              } catch (IllegalArgumentException ex) {
                log.debug("Ignoring unknown role code: {}", e.getCode(), ex);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
