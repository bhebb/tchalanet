package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class TenantUserNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.tenant_user";
  }

  @Override
  public boolean supports(Object event) {
    return simpleNameStartsWith(event, "TenantUser");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    return Stream.of(intent(
        event,
        "tenant_user.event",
        NotificationSeverity.INFO,
        NotificationKind.INFO,
        NotificationCategory.USER,
        "Tenant user update",
        event.getClass().getSimpleName()));
  }
}
