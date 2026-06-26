package com.tchalanet.server.platform.notification.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPublicationJpaRepository
    extends JpaRepository<NotificationPublicationJpaEntity, UUID> {

  Optional<NotificationPublicationJpaEntity>
      findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(UUID notificationId);

  boolean existsByNotificationIdAndDeletedAtIsNull(UUID notificationId);
}
