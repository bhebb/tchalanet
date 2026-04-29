package com.tchalanet.server.core.notification.infra.persistence;

import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

  Optional<NotificationJpaEntity> findFirstByDedupeKeyAndDeletedAtIsNull(String dedupeKey);

  @Query(
      """
      select n from NotificationJpaEntity n
       where n.deletedAt is null
         and (:status is null or n.status = :status)
         and (:category is null or n.category = :category)
         and (:kind is null or n.kind = :kind)
         and (:severity is null or n.severity = :severity)
         and (n.expiresAt is null or n.expiresAt > :now)
         and (
              n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.TENANT
           or n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.PLATFORM
           or (n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.USER and n.audienceValue = :userValue)
           or (n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.ROLE and n.audienceValue = :roleCode)
         )
       order by n.createdAt desc
      """)
  Page<NotificationJpaEntity> searchVisible(
      @Param("userValue") String userValue,
      @Param("roleCode") String roleCode,
      @Param("status") NotificationStatus status,
      @Param("category") NotificationCategory category,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      """
      select count(n) from NotificationJpaEntity n
       where n.deletedAt is null
         and (:status is null or n.status = :status)
         and (:kind is null or n.kind = :kind)
         and (:severity is null or n.severity = :severity)
         and (n.expiresAt is null or n.expiresAt > :now)
         and (
              n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.TENANT
           or n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.PLATFORM
           or (n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.USER and n.audienceValue = :userValue)
           or (n.audienceType = com.tchalanet.server.core.notification.domain.model.NotificationAudienceType.ROLE and n.audienceValue = :roleCode)
         )
      """)
  long countVisible(
      @Param("userValue") String userValue,
      @Param("roleCode") String roleCode,
      @Param("status") NotificationStatus status,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.READ,
             n.readAt = :readAt
       where n.id = :id
         and n.deletedAt is null
         and n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.UNREAD
      """)
  int markRead(@Param("id") UUID id, @Param("readAt") Instant readAt);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.READ,
             n.readAt = :readAt
       where n.id in :ids
         and n.deletedAt is null
         and n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.UNREAD
      """)
  int markReadAll(@Param("ids") Collection<UUID> ids, @Param("readAt") Instant readAt);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.ARCHIVED,
             n.archivedAt = :archivedAt
       where n.id = :id
         and n.deletedAt is null
         and n.status <> com.tchalanet.server.core.notification.domain.model.NotificationStatus.ARCHIVED
      """)
  int archive(@Param("id") UUID id, @Param("archivedAt") Instant archivedAt);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.ARCHIVED,
             n.archivedAt = :archivedAt
       where n.id in :ids
         and n.deletedAt is null
         and n.status <> com.tchalanet.server.core.notification.domain.model.NotificationStatus.ARCHIVED
      """)
  int archiveAll(@Param("ids") Collection<UUID> ids, @Param("archivedAt") Instant archivedAt);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.core.notification.domain.model.NotificationStatus.EXPIRED
       where n.deletedAt is null
         and n.expiresAt is not null
         and n.expiresAt <= :now
         and n.status not in (
           com.tchalanet.server.core.notification.domain.model.NotificationStatus.ARCHIVED,
           com.tchalanet.server.core.notification.domain.model.NotificationStatus.EXPIRED
         )
      """)
  int expire(@Param("now") Instant now);
}
