package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class OpsResourceNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.ops.resource";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("OpsJobStaleEvent")
        || name.equals("OpsJobNeverRunEvent")
        || name.equals("OpsGateDisabledEvent")
        || name.equals("OpsResourceCriticalEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    var critical = name.equals("OpsResourceCriticalEvent");
    return Stream.of(platformIntent(
        event,
        templateKey(name),
        critical ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
        critical ? NotificationKind.SYSTEM_ERROR : NotificationKind.ACTION_REQUIRED,
        NotificationCategory.SYSTEM,
        title(name),
        name));
  }

  private String templateKey(String eventName) {
    return switch (eventName) {
      case "OpsJobStaleEvent" -> "notification.system.ops.job_stale";
      case "OpsJobNeverRunEvent" -> "notification.system.ops.job_never_run";
      case "OpsGateDisabledEvent" -> "notification.system.ops.gate_disabled";
      case "OpsResourceCriticalEvent" -> "notification.system.ops.resource_critical";
      default -> "notification.system.ops.update";
    };
  }

  private String title(String eventName) {
    return switch (eventName) {
      case "OpsJobStaleEvent" -> "Ops job stale";
      case "OpsJobNeverRunEvent" -> "Ops job never run";
      case "OpsGateDisabledEvent" -> "Ops gate disabled";
      case "OpsResourceCriticalEvent" -> "Ops resource critical";
      default -> "Ops update";
    };
  }
}
