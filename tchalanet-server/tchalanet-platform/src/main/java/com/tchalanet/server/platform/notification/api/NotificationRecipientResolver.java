package com.tchalanet.server.platform.notification.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.view.NotificationRecipientContact;
import java.util.List;
import java.util.Set;

public interface NotificationRecipientResolver {

  default boolean supportsAudience(NotificationAudienceType audienceType) {
    return false;
  }

  default boolean supportsTarget(NotificationActorType actorType) {
    return false;
  }

  default List<NotificationRecipientContact> resolveAudience(
      TenantId tenantId, NotificationAudienceType audienceType) {
    return List.of();
  }

  default List<NotificationRecipientContact> resolveTargets(
      TenantId tenantId, Set<NotificationTarget> targets) {
    return List.of();
  }
}
