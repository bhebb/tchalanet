package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPublicationJpaRepository
    extends JpaRepository<NotificationPublicationJpaEntity, UUID> {

  Optional<NotificationPublicationJpaEntity>
      findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(UUID notificationId);

  List<NotificationPublicationJpaEntity>
      findByNotificationIdInAndDeletedAtIsNullOrderByPublicationNoDesc(Collection<UUID> notificationIds);

  boolean existsByNotificationIdAndDeletedAtIsNull(UUID notificationId);

  @Modifying
  @Query(
      """
      update NotificationPublicationJpaEntity p
         set p.status = :status
       where p.notificationId = :notificationId
         and p.deletedAt is null
         and p.status = com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus.PUBLISHED
      """)
  int updatePublishedStatus(
      @Param("notificationId") UUID notificationId,
      @Param("status") NotificationPublicationStatus status);

  @Modifying
  @Query(
      """
      update NotificationPublicationJpaEntity p
         set p.status = com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus.EXPIRED
       where p.deletedAt is null
         and p.status = com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus.PUBLISHED
         and p.expiresAt is not null
         and p.expiresAt <= :now
      """)
  int updatePublishedStatusForExpired(@Param("now") Instant now);
}
