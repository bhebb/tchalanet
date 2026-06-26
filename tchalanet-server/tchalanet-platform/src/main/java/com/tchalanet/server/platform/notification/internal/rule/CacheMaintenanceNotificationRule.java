package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class CacheMaintenanceNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.ops.cache_maintenance";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("CacheClearedEvent")
        || name.equals("MaintenanceScheduledEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    var maintenance = name.equals("MaintenanceScheduledEvent");
    return Stream.of(platformIntent(
        event,
        maintenance
            ? "notification.system.maintenance.scheduled"
            : "notification.system.cache.cleared",
        maintenance ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
        maintenance ? NotificationKind.ACTION_REQUIRED : NotificationKind.INFO,
        NotificationCategory.SYSTEM,
        maintenance ? "Maintenance scheduled" : "Cache cleared",
        name));
  }
}
