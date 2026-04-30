package com.tchalanet.server.catalog.settings.internal.batch;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.batch.gate.BatchGateFlagStore;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettingsBatchGateFlagStore implements BatchGateFlagStore {

  private static final String NAMESPACE = "batch";

  private final SettingRepository settingRepository;

  @Override
  public Optional<Boolean> findTenantFlag(JobKey jobKey, TenantId tenantId) {
    return findFlag(SettingLevel.TENANT, tenantId, jobKey);
  }

  @Override
  public Optional<Boolean> findGlobalFlag(JobKey jobKey) {
    return findFlag(SettingLevel.GLOBAL, null, jobKey);
  }

  private Optional<Boolean> findFlag(SettingLevel level, TenantId tenantId, JobKey jobKey) {
    String settingKey = "jobs." + jobKey.value() + ".enabled";
    return settingRepository
        .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
            level,
            tenantId != null ? tenantId.value() : null,
            null,
            null,
            NAMESPACE,
            settingKey)
        .flatMap(entity -> parseBool(entity.getSettingValue(), level, tenantId, settingKey));
  }

  private Optional<Boolean> parseBool(
      String raw, SettingLevel level, TenantId tenantId, String settingKey) {
    if (raw == null) {
      return Optional.empty();
    }
    return switch (raw.trim().toLowerCase()) {
      case "true", "1", "yes" -> Optional.of(Boolean.TRUE);
      case "false", "0", "no" -> Optional.of(Boolean.FALSE);
      default -> {
        log.warn(
            "Invalid batch gate boolean setting namespace={} settingKey={} level={} tenantId={} rawValue={}",
            NAMESPACE,
            settingKey,
            level,
            tenantId == null ? null : tenantId.value(),
            raw);
        yield Optional.empty();
      }
    };
  }
}
