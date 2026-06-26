package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationUserStateJpaRepository
    extends JpaRepository<NotificationUserStateJpaEntity, UUID> {

  Optional<NotificationUserStateJpaEntity>
      findFirstByPublicationIdAndActorTypeAndActorIdAndDeletedAtIsNull(
          UUID publicationId, NotificationActorType actorType, UUID actorId);

  @Modifying
  @Query(
      """
      update NotificationUserStateJpaEntity s
         set s.readAt = coalesce(s.readAt, :now)
       where s.publicationId = :publicationId
         and s.actorType = :actorType
         and s.actorId = :actorId
         and s.deletedAt is null
      """)
  int markRead(
      @Param("publicationId") UUID publicationId,
      @Param("actorType") NotificationActorType actorType,
      @Param("actorId") UUID actorId,
      @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationUserStateJpaEntity s
         set s.dismissedAt = coalesce(s.dismissedAt, :now),
             s.readAt = coalesce(s.readAt, :now)
       where s.publicationId = :publicationId
         and s.actorType = :actorType
         and s.actorId = :actorId
         and s.deletedAt is null
      """)
  int dismiss(
      @Param("publicationId") UUID publicationId,
      @Param("actorType") NotificationActorType actorType,
      @Param("actorId") UUID actorId,
      @Param("now") Instant now);
}
