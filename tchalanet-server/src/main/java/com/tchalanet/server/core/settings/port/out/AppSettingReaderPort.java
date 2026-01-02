package com.tchalanet.server.core.settings.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.settings.infra.persistence.AppSettingEntity;
import java.util.List;

public interface AppSettingReaderPort {
  List<AppSettingEntity> findGlobal(List<String> namespaces);

  List<AppSettingEntity> findForTenant(TenantId tenantId, List<String> namespaces);

  List<AppSettingEntity> findForOutlet(
      TenantId tenantId, OutletId outletId, List<String> namespaces);

  List<AppSettingEntity> findForTerminal(
      TenantId tenantId, TerminalId terminalId, List<String> namespaces);
}
