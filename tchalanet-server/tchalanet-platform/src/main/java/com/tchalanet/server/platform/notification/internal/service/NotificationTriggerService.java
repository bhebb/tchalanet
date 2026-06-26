package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationTriggerLogJpaEntity;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationTriggerLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTriggerService {

  private final NotificationTriggerLogJpaRepository triggerLogs;

  @TchTx
  public boolean markTriggeredIfAbsent(
      String triggerKey, String sourceType, String sourceId, TenantId tenantId) {
    var normalizedTriggerKey = requireStableValue(triggerKey, "triggerKey");
    var normalizedSourceType = requireStableValue(sourceType, "sourceType");
    var normalizedSourceId = requireStableValue(sourceId, "sourceId");

    var existing =
        triggerLogs.findFirstByTriggerKeyAndSourceTypeAndSourceIdAndDeletedAtIsNull(
            normalizedTriggerKey, normalizedSourceType, normalizedSourceId);
    if (existing.isPresent()) {
      return false;
    }

    var entity = new NotificationTriggerLogJpaEntity();
    entity.setTenantId(tenantId == null ? null : tenantId.value());
    entity.setTriggerKey(normalizedTriggerKey);
    entity.setSourceType(normalizedSourceType);
    entity.setSourceId(normalizedSourceId);
    try {
      triggerLogs.save(entity);
      return true;
    } catch (DataIntegrityViolationException ex) {
      log.debug(
          "Notification trigger already claimed triggerKey={} sourceType={} sourceId={}",
          normalizedTriggerKey,
          normalizedSourceType,
          normalizedSourceId);
      return false;
    }
  }

  private static String requireStableValue(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("notification.trigger." + field + "_required");
    }
    return value.trim();
  }
}
