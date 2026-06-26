package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
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
public interface NotificationRecipientJpaRepository
    extends JpaRepository<NotificationRecipientJpaEntity, UUID> {

  Optional<NotificationRecipientJpaEntity>
      findFirstByNotificationIdAndRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNull(
          UUID notificationId, NotificationActorType actorType, UUID actorId);

  List<NotificationRecipientJpaEntity>
      findByRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNullOrderByCreatedAtDesc(
          NotificationActorType actorType, UUID actorId);

  List<NotificationRecipientJpaEntity>
      findByNotificationIdInAndRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNull(
          Collection<UUID> notificationIds, NotificationActorType actorType, UUID actorId);

  @Modifying
  @Query(
      """
      update NotificationRecipientJpaEntity r
         set r.readAt = coalesce(r.readAt, :now)
       where r.notificationId = :notificationId
         and r.recipientActorType = :actorType
         and r.recipientActorId = :actorId
         and r.deletedAt is null
      """)
  int markRead(
      @Param("notificationId") UUID notificationId,
      @Param("actorType") NotificationActorType actorType,
      @Param("actorId") UUID actorId,
      @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationRecipientJpaEntity r
         set r.dismissedAt = coalesce(r.dismissedAt, :now),
             r.readAt = coalesce(r.readAt, :now)
       where r.notificationId = :notificationId
         and r.recipientActorType = :actorType
         and r.recipientActorId = :actorId
         and r.deletedAt is null
      """)
  int dismiss(
      @Param("notificationId") UUID notificationId,
      @Param("actorType") NotificationActorType actorType,
      @Param("actorId") UUID actorId,
      @Param("now") Instant now);
}
