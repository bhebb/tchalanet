package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class OfflineSyncNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.offlinesync";
  }

  @Override
  public boolean supports(Object event) {
    return simpleNameStartsWith(event, "Offline");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    return Stream.of(intent(
        event,
        "offlinesync.event",
        NotificationSeverity.WARNING,
        NotificationKind.ACTION_REQUIRED,
        NotificationCategory.SYSTEM,
        "Offline sync update",
        event.getClass().getSimpleName()));
  }
}
