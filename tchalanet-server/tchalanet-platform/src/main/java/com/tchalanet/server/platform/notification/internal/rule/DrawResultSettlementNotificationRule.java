package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class DrawResultSettlementNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.draw_result.settlement";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("ResultProviderDownEvent")
        || name.equals("ResultProviderDegradedEvent")
        || name.equals("DrawResultMissingEvent")
        || name.equals("DrawResultCorrectedEvent")
        || name.equals("TicketResultCorrectedEvent")
        || name.equals("SettlementFailedEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    var critical = name.equals("ResultProviderDownEvent") || name.equals("SettlementFailedEvent");
    var corrected = name.equals("DrawResultCorrectedEvent") || name.equals("TicketResultCorrectedEvent");
    return Stream.of(intent(
        event,
        templateKey(name),
        critical ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
        critical ? NotificationKind.SYSTEM_ERROR : NotificationKind.ACTION_REQUIRED,
        corrected ? NotificationCategory.RESULT : NotificationCategory.DRAW,
        title(name),
        name));
  }

  private String templateKey(String eventName) {
    return switch (eventName) {
      case "ResultProviderDownEvent" -> "notification.system.result.provider_down";
      case "ResultProviderDegradedEvent" -> "notification.system.result.provider_degraded";
      case "DrawResultMissingEvent" -> "notification.system.draw_result.missing";
      case "DrawResultCorrectedEvent", "TicketResultCorrectedEvent" ->
          "notification.system.draw_result.corrected";
      case "SettlementFailedEvent" -> "notification.system.settlement.failed";
      default -> "notification.system.draw_result.update";
    };
  }

  private String title(String eventName) {
    return switch (eventName) {
      case "ResultProviderDownEvent" -> "Result provider down";
      case "ResultProviderDegradedEvent" -> "Result provider degraded";
      case "DrawResultMissingEvent" -> "Draw result missing";
      case "DrawResultCorrectedEvent", "TicketResultCorrectedEvent" -> "Draw result corrected";
      case "SettlementFailedEvent" -> "Settlement failed";
      default -> "Draw result update";
    };
  }
}
