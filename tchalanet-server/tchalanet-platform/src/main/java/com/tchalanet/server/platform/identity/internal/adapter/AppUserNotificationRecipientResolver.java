package com.tchalanet.server.platform.identity.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.notification.api.NotificationRecipientResolver;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.view.NotificationRecipientContact;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AppUserNotificationRecipientResolver implements NotificationRecipientResolver {

  private final IdentityApi identityApi;

  @Override
  public boolean supportsAudience(NotificationAudienceType audienceType) {
    return switch (audienceType) {
      case PLATFORM_ADMINS, TENANT_ADMINS, TENANT_APP_USERS -> true;
      case SPECIFIC_ACTORS, ALL_APP_USERS, TENANT_SELLER_TERMINALS -> false;
    };
  }

  @Override
  public boolean supportsTarget(NotificationActorType actorType) {
    return actorType == NotificationActorType.APP_USER;
  }

  @Override
  public List<NotificationRecipientContact> resolveAudience(
      TenantId tenantId, NotificationAudienceType audienceType) {
    return switch (audienceType) {
      case PLATFORM_ADMINS -> identityApi.listPlatformAdminsForNotificationDelivery().stream()
          .map(user -> toContact(tenantId, user))
          .toList();
      case TENANT_ADMINS -> identityApi.listTenantAdminsForNotificationDelivery(tenantId).stream()
          .map(user -> toContact(tenantId, user))
          .toList();
      case TENANT_APP_USERS -> identityApi.listTenantUsersForNotificationDelivery(tenantId).stream()
          .map(user -> toContact(tenantId, user))
          .toList();
      case SPECIFIC_ACTORS, ALL_APP_USERS, TENANT_SELLER_TERMINALS -> List.of();
    };
  }

  @Override
  public List<NotificationRecipientContact> resolveTargets(
      TenantId tenantId, Set<NotificationTarget> targets) {
    if (targets == null || targets.isEmpty()) {
      return List.of();
    }
    return targets.stream()
        .filter(target -> target.actorType() == NotificationActorType.APP_USER)
        .map(target -> identityApi.findAppUser(target.actorId()))
        .flatMap(java.util.Optional::stream)
        .map(user -> toContact(tenantId, user))
        .toList();
  }

  private NotificationRecipientContact toContact(TenantId tenantId, AppUserView user) {
    return new NotificationRecipientContact(
        tenantId,
        NotificationTarget.appUser(user.id().value()),
        user.id(),
        user.email(),
        user.phone());
  }
}
