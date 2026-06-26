package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
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
         and (
              :searchPattern is null
           or lower(n.type) like :searchPattern
           or lower(coalesce(n.sourceType, '')) like :searchPattern
           or lower(coalesce(n.sourceId, '')) like :searchPattern
           or lower(coalesce(n.dedupeKey, '')) like :searchPattern
           or lower(coalesce(n.titleKey, '')) like :searchPattern
           or lower(coalesce(n.messageKey, '')) like :searchPattern
           or lower(coalesce(n.titleText, '')) like :searchPattern
           or lower(coalesce(n.messageText, '')) like :searchPattern
           or lower(coalesce(n.actionRoute, '')) like :searchPattern
           or lower(coalesce(n.actionUrl, '')) like :searchPattern
         )
         and (n.expiresAt is null or n.expiresAt > :now)
         and (
              n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.ALL_APP_USERS
           or n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_APP_USERS
           or (n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_ADMINS and :roleCode = 'TENANT_ADMIN')
           or (n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.PLATFORM_ADMINS and :roleCode = 'SUPER_ADMIN')
           or exists (
              select 1 from NotificationRecipientJpaEntity r
               where r.notificationId = n.id
                 and r.recipientActorType = com.tchalanet.server.platform.notification.api.model.NotificationActorType.APP_USER
                 and r.recipientActorId = :userId
                 and r.deletedAt is null
           )
         )
       order by n.createdAt desc
      """)
  Page<NotificationJpaEntity> searchVisible(
      @Param("userId") UUID userId,
      @Param("roleCode") String roleCode,
      @Param("status") NotificationStatus status,
      @Param("category") NotificationCategory category,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("searchPattern") String searchPattern,
      @Param("now") Instant now,
      Pageable pageable);

  @Query(
      """
      select n from NotificationJpaEntity n
       where n.deletedAt is null
         and (:status is null or n.status = :status)
         and (:category is null or n.category = :category)
         and (:kind is null or n.kind = :kind)
         and (:severity is null or n.severity = :severity)
         and (
              :searchPattern is null
           or lower(n.type) like :searchPattern
           or lower(coalesce(n.sourceType, '')) like :searchPattern
           or lower(coalesce(n.sourceId, '')) like :searchPattern
           or lower(coalesce(n.dedupeKey, '')) like :searchPattern
           or lower(coalesce(n.titleKey, '')) like :searchPattern
           or lower(coalesce(n.messageKey, '')) like :searchPattern
           or lower(coalesce(n.titleText, '')) like :searchPattern
           or lower(coalesce(n.messageText, '')) like :searchPattern
           or lower(coalesce(n.actionRoute, '')) like :searchPattern
           or lower(coalesce(n.actionUrl, '')) like :searchPattern
         )
         and (n.expiresAt is null or n.expiresAt > :now)
         and (
              n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_SELLER_TERMINALS
           or exists (
              select 1 from NotificationRecipientJpaEntity r
               where r.notificationId = n.id
                 and r.recipientActorType = com.tchalanet.server.platform.notification.api.model.NotificationActorType.SELLER_TERMINAL
                 and r.recipientActorId = :terminalId
                 and r.deletedAt is null
           )
         )
       order by n.createdAt desc
      """)
  Page<NotificationJpaEntity> searchVisibleToTerminal(
      @Param("terminalId") UUID terminalId,
      @Param("status") NotificationStatus status,
      @Param("category") NotificationCategory category,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("searchPattern") String searchPattern,
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
              n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.ALL_APP_USERS
           or n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_APP_USERS
           or (n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_ADMINS and :roleCode = 'TENANT_ADMIN')
           or (n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.PLATFORM_ADMINS and :roleCode = 'SUPER_ADMIN')
           or exists (
              select 1 from NotificationRecipientJpaEntity r
               where r.notificationId = n.id
                 and r.recipientActorType = com.tchalanet.server.platform.notification.api.model.NotificationActorType.APP_USER
                 and r.recipientActorId = :userId
                 and r.deletedAt is null
           )
         )
      """)
  long countVisible(
      @Param("userId") UUID userId,
      @Param("roleCode") String roleCode,
      @Param("status") NotificationStatus status,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("now") Instant now);

  @Query(
      """
      select count(n) from NotificationJpaEntity n
       where n.deletedAt is null
         and (:status is null or n.status = :status)
         and (:kind is null or n.kind = :kind)
         and (:severity is null or n.severity = :severity)
         and (n.expiresAt is null or n.expiresAt > :now)
         and (
              n.audienceType = com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.TENANT_SELLER_TERMINALS
           or exists (
              select 1 from NotificationRecipientJpaEntity r
               where r.notificationId = n.id
                 and r.recipientActorType = com.tchalanet.server.platform.notification.api.model.NotificationActorType.SELLER_TERMINAL
                 and r.recipientActorId = :terminalId
                 and r.deletedAt is null
           )
         )
      """)
  long countVisibleToTerminal(
      @Param("terminalId") UUID terminalId,
      @Param("status") NotificationStatus status,
      @Param("kind") NotificationKind kind,
      @Param("severity") NotificationSeverity severity,
      @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.EXPIRED
       where n.deletedAt is null
         and n.expiresAt is not null
         and n.expiresAt <= :now
         and n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.PUBLISHED
      """)
  int expire(@Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.PUBLISHED,
             n.publishedAt = coalesce(n.publishedAt, :now),
             n.updatedAt = :now
       where n.id = :notificationId
         and n.deletedAt is null
         and n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.DRAFT
      """)
  int publishDraft(@Param("notificationId") UUID notificationId, @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.CANCELLED,
             n.cancelledAt = :now,
             n.cancelledReason = :reason,
             n.updatedAt = :now
       where n.id = :notificationId
         and n.deletedAt is null
         and n.status <> com.tchalanet.server.platform.notification.api.model.NotificationStatus.PURGED
      """)
  int cancel(
      @Param("notificationId") UUID notificationId,
      @Param("reason") String reason,
      @Param("now") Instant now);

  @Modifying
  @Query(
      """
      update NotificationJpaEntity n
         set n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.PURGED,
             n.purgedAt = :now,
             n.updatedAt = :now
       where n.deletedAt is null
         and n.status <> com.tchalanet.server.platform.notification.api.model.NotificationStatus.PURGED
         and (
              (
                n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.EXPIRED
                and coalesce(n.expiresAt, n.updatedAt, n.createdAt) <= :lifecycleCutoff
              )
           or (
                n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.CANCELLED
                and coalesce(n.cancelledAt, n.updatedAt, n.createdAt) <= :lifecycleCutoff
              )
           or (n.expiresAt is not null and n.expiresAt <= :lifecycleCutoff)
         )
      """)
  int purgeLifecycle(
      @Param("now") Instant now,
      @Param("lifecycleCutoff") Instant lifecycleCutoff);

  @Query(
      """
      select count(n) from NotificationJpaEntity n
       where n.deletedAt is null
         and n.status <> com.tchalanet.server.platform.notification.api.model.NotificationStatus.PURGED
         and (
              (
                n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.EXPIRED
                and coalesce(n.expiresAt, n.updatedAt, n.createdAt) <= :lifecycleCutoff
              )
           or (
                n.status = com.tchalanet.server.platform.notification.api.model.NotificationStatus.CANCELLED
                and coalesce(n.cancelledAt, n.updatedAt, n.createdAt) <= :lifecycleCutoff
              )
           or (n.expiresAt is not null and n.expiresAt <= :lifecycleCutoff)
         )
      """)
  long countPurgeCandidates(@Param("lifecycleCutoff") Instant lifecycleCutoff);
}
