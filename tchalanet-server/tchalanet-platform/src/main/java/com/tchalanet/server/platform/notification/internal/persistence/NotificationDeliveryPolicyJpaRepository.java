package com.tchalanet.server.platform.notification.internal.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDeliveryPolicyJpaRepository
    extends JpaRepository<NotificationDeliveryPolicyJpaEntity, UUID> {

  List<NotificationDeliveryPolicyJpaEntity> findByNotificationIdAndEnabledTrueAndDeletedAtIsNull(
      UUID notificationId);
}
