package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SellerTerminalLifecycleNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.seller_terminal.lifecycle";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("SellerTerminalCreatedEvent")
        || name.equals("SellerTerminalPinResetEvent")
        || name.equals("SellerTerminalBlockedEvent")
        || name.equals("SellerTerminalDisabledEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    var actionRequired =
        name.equals("SellerTerminalPinResetEvent")
            || name.equals("SellerTerminalBlockedEvent")
            || name.equals("SellerTerminalDisabledEvent");
    return Stream.of(intent(
        event,
        templateKey(name),
        actionRequired ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
        actionRequired ? NotificationKind.ACTION_REQUIRED : NotificationKind.INFO,
        NotificationCategory.TERMINAL,
        title(name),
        name));
  }

  private String templateKey(String eventName) {
    return switch (eventName) {
      case "SellerTerminalPinResetEvent" -> "notification.system.seller_terminal.pin_reset";
      case "SellerTerminalBlockedEvent", "SellerTerminalDisabledEvent" ->
          "notification.system.seller_terminal.blocked";
      default -> "notification.system.seller_terminal.lifecycle";
    };
  }

  private String title(String eventName) {
    return switch (eventName) {
      case "SellerTerminalCreatedEvent" -> "Seller terminal created";
      case "SellerTerminalPinResetEvent" -> "Seller terminal PIN reset";
      case "SellerTerminalBlockedEvent" -> "Seller terminal blocked";
      case "SellerTerminalDisabledEvent" -> "Seller terminal disabled";
      default -> "Seller terminal update";
    };
  }
}
