package com.tchalanet.server.core.settings;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.core.settings.infra.persistence.AppSettingEntity;
import com.tchalanet.server.core.settings.port.out.AppSettingReaderPort;
import com.tchalanet.server.core.settings.query.ResolveAppSettingsQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppSettingsResolver {

  private final AppSettingReaderPort repo;

  @Cacheable(cacheNames = "app_settings_resolved", key = "T(AppSettingsCacheKey).of(#q)")
  public List<ResolvedSettingDto> resolve(ResolveAppSettingsQuery q) {
    TenantId tenantId = q.tenantId();
    List<String> namespaces =
        (q.namespaces() == null || q.namespaces().isEmpty()) ? List.of() : q.namespaces();

    // map slotKey = namespace + '\u0000' + settingKey
    Map<String, ResolvedSettingDto> resolved = new LinkedHashMap<>();

    // 1) GLOBAL
    merge(resolved, repo.findGlobal(namespaces), "GLOBAL");

    // 2) TENANT
    merge(resolved, repo.findForTenant(tenantId, namespaces), "TENANT");

    // 3) OUTLET (si fourni)
    if (q.outletId() != null) {
      merge(resolved, repo.findForOutlet(tenantId, q.outletId(), namespaces), "OUTLET");
    }

    // 4) TERMINAL (si fourni)
    if (q.terminalId() != null) {
      merge(resolved, repo.findForTerminal(tenantId, q.terminalId(), namespaces), "TERMINAL");
    }

    return List.copyOf(resolved.values());
  }

  private void merge(
      Map<String, ResolvedSettingDto> resolved,
      List<AppSettingEntity> entities,
      String effectiveLevel) {
    for (var e : entities) {
      String k = keyOf(e.getNamespace(), e.getSettingKey());
      // override allowed: later calls overwrite earlier ones
      resolved.put(
          k,
          new ResolvedSettingDto(
              e.getNamespace(),
              e.getSettingKey(),
              safeType(e.getValueType()),
              e.getSettingValue(),
              effectiveLevel));
    }
  }

  private static String keyOf(String ns, String key) {
    return ns + "\u0000" + key;
  }

  private static AppSettingValueType safeType(Object raw) {
    if (raw instanceof AppSettingValueType t) return t;
    if (raw == null) return AppSettingValueType.STRING;
    try {
      return AppSettingValueType.valueOf(raw.toString());
    } catch (Exception ex) {
      return AppSettingValueType.STRING;
    }
  }
}
