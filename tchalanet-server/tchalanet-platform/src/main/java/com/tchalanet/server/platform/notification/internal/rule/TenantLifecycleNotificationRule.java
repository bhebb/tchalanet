package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class TenantLifecycleNotificationRule extends AbstractNotificationRule {

  @Override
  public String handlerKey() {
    return "notification.tenant.lifecycle";
  }

  @Override
  public boolean supports(Object event) {
    if (event == null) {
      return false;
    }
    var name = event.getClass().getSimpleName();
    return name.equals("TenantCreatedEvent")
        || name.equals("TenantAdminCreatedEvent")
        || name.equals("TenantAdminInvitedEvent")
        || name.equals("TenantOnboardingIncompleteEvent");
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var name = event.getClass().getSimpleName();
    var onboarding = name.equals("TenantOnboardingIncompleteEvent");
    return Stream.of(intent(
        event,
        onboarding
            ? "notification.system.tenant.onboarding_incomplete"
            : "notification.system.tenant.lifecycle",
        onboarding ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
        onboarding ? NotificationKind.ACTION_REQUIRED : NotificationKind.INFO,
        NotificationCategory.TENANT_CONFIG,
        title(name),
        name));
  }

  private String title(String eventName) {
    return switch (eventName) {
      case "TenantCreatedEvent" -> "Tenant created";
      case "TenantAdminCreatedEvent" -> "Tenant admin created";
      case "TenantAdminInvitedEvent" -> "Tenant admin invited";
      case "TenantOnboardingIncompleteEvent" -> "Tenant onboarding incomplete";
      default -> "Tenant lifecycle update";
    };
  }
}
