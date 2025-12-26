package com.tchalanet.server.common.settings.infra.persistence;

import com.tchalanet.server.common.persistence.AppSettingEntity;
import com.tchalanet.server.common.persistence.AppSettingRepository;
import com.tchalanet.server.common.settings.AppSettingLevel;
import com.tchalanet.server.common.settings.port.out.AppSettingReaderPort;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppSettingRepositoryAdapter implements AppSettingReaderPort {

  private final AppSettingRepository repo;

  @Override
  public List<AppSettingEntity> findGlobal(List<String> namespaces) {
    if (namespaces == null || namespaces.isEmpty()) return List.of();
    return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
        AppSettingLevel.GLOBAL, namespaces);
  }

  @Override
  public List<AppSettingEntity> findForTenant(TenantId tenantId, List<String> namespaces) {
    if (namespaces == null || namespaces.isEmpty()) return List.of();
    return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
        AppSettingLevel.TENANT, tenantId.uuid(), namespaces);
  }

  @Override
  public List<AppSettingEntity> findForOutlet(
      TenantId tenantId, OutletId outletId, List<String> namespaces) {
    if (namespaces == null || namespaces.isEmpty()) return List.of();
    return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndNamespaceIn(
        AppSettingLevel.OUTLET, tenantId.uuid(), outletId.uuid(), namespaces);
  }

  @Override
  public List<AppSettingEntity> findForTerminal(
      TenantId tenantId, TerminalId terminalId, List<String> namespaces) {
    if (namespaces == null || namespaces.isEmpty()) return List.of();
    return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalIdAndNamespaceIn(
        AppSettingLevel.TERMINAL, tenantId.uuid(), terminalId.uuid(), namespaces);
  }
}
