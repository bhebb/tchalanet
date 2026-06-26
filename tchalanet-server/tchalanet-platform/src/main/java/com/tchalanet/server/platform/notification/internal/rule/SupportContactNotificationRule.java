package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SupportContactNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.support.contact";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("PublicContactReceivedEvent")
        || name.equals("PublicContactRequestCreatedEvent")
        || name.equals("TenantSupportRequestCreatedEvent")
        || name.equals("SellerTerminalHelpRequestedEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    if (name.startsWith("PublicContact")) {
      return Stream.of(platformIntent(
          event,
          "notification.system.support.public_contact_received",
          NotificationSeverity.INFO,
          NotificationKind.ACTION_REQUIRED,
          NotificationCategory.SYSTEM,
          "Public contact received",
          name));
    }
    return Stream.of(intent(
        event,
        name.equals("SellerTerminalHelpRequestedEvent")
            ? "notification.system.seller_terminal.help_requested"
            : "notification.system.support.tenant_request",
        NotificationSeverity.WARNING,
        NotificationKind.ACTION_REQUIRED,
        NotificationCategory.SYSTEM,
        name.equals("SellerTerminalHelpRequestedEvent")
            ? "Seller terminal help requested"
            : "Tenant support request",
        name));
  }
}
