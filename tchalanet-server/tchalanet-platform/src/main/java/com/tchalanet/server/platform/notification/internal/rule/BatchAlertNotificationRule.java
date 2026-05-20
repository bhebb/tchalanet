package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class BatchAlertNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.batch.failed";
  }

  @Override
  public boolean supports(Object event) {
    return simpleName(event, "BatchFailedEvent") || simpleNameStartsWith(event, "Batch");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    return Stream.of(platformIntent(
        event,
        "batch.failed",
        NotificationSeverity.ERROR,
        NotificationKind.SYSTEM_ERROR,
        NotificationCategory.BATCH,
        "Batch alert",
        event.getClass().getSimpleName()));
  }
}
