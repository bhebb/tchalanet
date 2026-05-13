package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class PayoutNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.payout";
  }

  @Override
  public boolean supports(Object event) {
    return event != null && event.getClass().getSimpleName().startsWith("Payout");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    return Stream.of(intent(
        event,
        "payout.event",
        NotificationSeverity.INFO,
        NotificationKind.INFO,
        NotificationCategory.PAYOUT,
        "Payout update",
        event.getClass().getSimpleName()));
  }
}
