package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.view.NotificationActionView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationUnreadCountView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.internal.mapper.NotificationMapper;
import com.tchalanet.server.platform.notification.internal.service.Notification;
import com.tchalanet.server.platform.notification.internal.service.NotificationReader;
import com.tchalanet.server.platform.notification.internal.service.NotificationWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceService
    implements NotificationWriter, NotificationReader {

  private final Clock clock;
  private final NotificationJpaRepository notifications;
  private final NotificationPublicationJpaRepository publications;
  private final NotificationUserStateJpaRepository userStates;
  private final NotificationRecipientJpaRepository recipients;
  private final NotificationTranslationJpaRepository translations;

  @Override
  public Optional<Notification> findByDedupeKey(String dedupeKey) {
    if (dedupeKey == null || dedupeKey.isBlank()) {
      return Optional.empty();
    }
    return notifications.findFirstByDedupeKeyAndDeletedAtIsNull(dedupeKey).map(NotificationMapper::toDomain);
  }

  @Override
  public Notification save(Notification notification) {
    var saved = notifications.save(NotificationMapper.toEntity(notification, null));
    var publication = ensurePublication(saved);
    ensureExplicitRecipients(saved, publication, notification.targets());
    return NotificationMapper.toDomain(saved);
  }

  @Override
  public int expire(Instant now) {
    return notifications.expire(now);
  }

  @Override
  public NotificationSummaryView summary(UserId userId, String roleCode) {
    var now = clock.instant();
    var unread =
        notifications.countVisible(
            userId == null ? null : userId.value(), roleCode, NotificationStatus.PUBLISHED, null, null, now);
    var critical =
        notifications.countVisible(
            userId == null ? null : userId.value(), roleCode, NotificationStatus.PUBLISHED, null, NotificationSeverity.CRITICAL, now);
    var actionRequired =
        notifications.countVisible(
            userId == null ? null : userId.value(), roleCode, NotificationStatus.PUBLISHED, NotificationKind.ACTION_REQUIRED, null, now);
    return new NotificationSummaryView(unread, critical, actionRequired, actionRequired > 0);
  }

  @Override
  public NotificationSummaryView summaryForTerminal(SellerTerminalId sellerTerminalId) {
    var now = clock.instant();
    var unread =
        notifications.countVisibleToTerminal(
            sellerTerminalId.value(), NotificationStatus.PUBLISHED, null, null, now);
    var critical =
        notifications.countVisibleToTerminal(
            sellerTerminalId.value(), NotificationStatus.PUBLISHED, null, NotificationSeverity.CRITICAL, now);
    var actionRequired =
        notifications.countVisibleToTerminal(
            sellerTerminalId.value(), NotificationStatus.PUBLISHED, NotificationKind.ACTION_REQUIRED, null, now);
    return new NotificationSummaryView(unread, critical, actionRequired, actionRequired > 0);
  }

  @Override
  public TchPage<NotificationItemView> list(
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest) {
    var page =
        notifications.searchVisible(
            userId == null ? null : userId.value(),
            roleCode,
            status.orElse(null),
            category.orElse(null),
            kind.orElse(null),
            severity.orElse(null),
            search == null ? null : search.likePattern(),
            clock.instant(),
            pageRequest.pageable());
    return TchPageMapper.map(page, NotificationMapper::toItemView);
  }

  @Override
  public TchPage<NotificationItemView> listForTerminal(
      SellerTerminalId sellerTerminalId,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest) {
    var page =
        notifications.searchVisibleToTerminal(
            sellerTerminalId.value(),
            status.orElse(null),
            category.orElse(null),
            kind.orElse(null),
            severity.orElse(null),
            search == null ? null : search.likePattern(),
            clock.instant(),
            pageRequest.pageable());
    return TchPageMapper.map(page, NotificationMapper::toItemView);
  }

  @Override
  public TchPage<NotificationItemView> listMyNotifications(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest) {
    var page =
        notifications.searchVisible(
            userId == null ? null : userId.value(),
            roleCode,
            status.orElse(NotificationStatus.PUBLISHED),
            category.orElse(null),
            kind.orElse(null),
            severity.orElse(null),
            search == null ? null : search.likePattern(),
            clock.instant(),
            pageRequest.pageable());
    var recipientByNotificationId = recipientByNotificationId(page.getContent().stream().map(NotificationJpaEntity::getId).toList(), actorType, actorId);
    return TchPageMapper.map(
        page, entity -> toPersonalItemView(entity, actorType, actorId, recipientByNotificationId.get(entity.getId())));
  }

  @Override
  public NotificationUnreadCountView countUnread(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode) {
    var page =
        notifications.searchVisible(
            userId == null ? null : userId.value(),
            roleCode,
            null,
            null,
            null,
            null,
            null,
            clock.instant(),
            PageRequest.of(0, 500));
    var unread =
        page.getContent().stream()
            .map(entity -> toPersonalItemView(entity, actorType, actorId, null))
            .filter(item -> item.readAt() == null && item.archivedAt() == null)
            .count();
    return new NotificationUnreadCountView(unread);
  }

  @Override
  public void markRead(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
    markPersonalState(notificationId.value(), actorType, actorId, false);
  }

  @Override
  public void dismiss(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
    markPersonalState(notificationId.value(), actorType, actorId, true);
  }

  @Override
  public void markAllRead(NotificationActorType actorType, UUID actorId, UserId userId, String roleCode) {
    var page =
        notifications.searchVisible(
            userId == null ? null : userId.value(),
            roleCode,
            null,
            null,
            null,
            null,
            null,
            clock.instant(),
            PageRequest.of(0, 500));
    page.getContent().forEach(entity -> markPersonalState(entity.getId(), actorType, actorId, false));
  }

  private NotificationItemView toPersonalItemView(
      NotificationJpaEntity entity,
      NotificationActorType actorType,
      UUID actorId,
      NotificationRecipientJpaEntity recipient) {
    var publication =
        publications.findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(entity.getId());
    var state =
        publication.flatMap(
            p ->
                userStates.findFirstByPublicationIdAndActorTypeAndActorIdAndDeletedAtIsNull(
                    p.getId(), actorType, actorId));
    var readAt =
        recipient != null
            ? recipient.getReadAt()
            : state.map(NotificationUserStateJpaEntity::getReadAt).orElse(null);
    var dismissedAt =
        recipient != null
            ? recipient.getDismissedAt()
            : state.map(NotificationUserStateJpaEntity::getDismissedAt).orElse(null);
    return new NotificationItemView(
        NotificationId.of(entity.getId()),
        entity.getSeverity(),
        entity.getKind(),
        entity.getCategory(),
        entity.getTitleKey(),
        entity.getMessageKey(),
        titleText(entity),
        messageText(entity),
        entity.getPayload(),
        new NotificationActionView(entity.getActionType(), entity.getActionUrl()),
        entity.getStatus(),
        readAt,
        dismissedAt,
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }

  private int markPersonalState(
      UUID notificationId, NotificationActorType actorType, UUID actorId, boolean dismiss) {
    var publication =
        publications.findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(notificationId);
    if (publication.isEmpty()) {
      return 0;
    }
    var recipient =
        recipients.findFirstByPublicationIdAndRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNull(
            publication.get().getId(), actorType, actorId);
    if (recipient.isPresent()) {
      var now = clock.instant();
      return dismiss
          ? recipients.dismissByPublication(publication.get().getId(), actorType, actorId, now)
          : recipients.markReadByPublication(publication.get().getId(), actorType, actorId, now);
    }
    var state =
        userStates
            .findFirstByPublicationIdAndActorTypeAndActorIdAndDeletedAtIsNull(
                publication.get().getId(), actorType, actorId)
            .orElseGet(() -> createUserState(publication.get(), actorType, actorId));
    var now = clock.instant();
    if (dismiss) {
      return userStates.dismiss(state.getPublicationId(), actorType, actorId, now);
    }
    return userStates.markRead(state.getPublicationId(), actorType, actorId, now);
  }

  private NotificationUserStateJpaEntity createUserState(
      NotificationPublicationJpaEntity publication, NotificationActorType actorType, UUID actorId) {
    var state = new NotificationUserStateJpaEntity();
    state.setNotificationId(publication.getNotificationId());
    state.setPublicationId(publication.getId());
    state.setTenantId(publication.getTenantId());
    state.setActorType(actorType);
    state.setActorId(actorId);
    return userStates.save(state);
  }

  private NotificationPublicationJpaEntity ensurePublication(NotificationJpaEntity notification) {
    var existing =
        publications.findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(
            notification.getId());
    if (existing.isPresent()) {
      return existing.get();
    }
    var publication = new NotificationPublicationJpaEntity();
    publication.setNotificationId(notification.getId());
    publication.setTenantId(notification.getTenantId());
    publication.setPublicationNo(1);
    publication.setStatus(NotificationPublicationStatus.PUBLISHED);
    publication.setAudienceType(publicationAudience(notification.getAudienceType()));
    publication.setPublishedAt(clock.instant());
    publication.setExpiresAt(notification.getExpiresAt());
    return publications.save(publication);
  }

  private void ensureExplicitRecipients(
      NotificationJpaEntity notification,
      NotificationPublicationJpaEntity publication,
      java.util.Set<NotificationTarget> targets) {
    if (notification.getAudienceType() != NotificationAudienceType.SPECIFIC_ACTORS
        || targets == null
        || targets.isEmpty()) {
      return;
    }
    var notificationTargets = explicitTargets(targets);
    for (var target : notificationTargets) {
      var exists =
          recipients.existsByPublicationIdAndRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNull(
              publication.getId(), target.actorType(), target.actorId());
      if (exists) {
        continue;
      }
      var recipient = new NotificationRecipientJpaEntity();
      recipient.setNotificationId(notification.getId());
      recipient.setPublicationId(publication.getId());
      recipient.setTenantId(notification.getTenantId());
      recipient.setRecipientActorType(target.actorType());
      recipient.setRecipientActorId(target.actorId());
      recipient.setDeliveredAt(publication.getPublishedAt());
      recipients.save(recipient);
    }
  }

  private java.util.Set<NotificationTarget> explicitTargets(java.util.Set<NotificationTarget> targets) {
    var notificationTargets = new java.util.LinkedHashSet<NotificationTarget>();
    targets.stream().filter(java.util.Objects::nonNull).forEach(notificationTargets::add);
    return java.util.Collections.unmodifiableSet(notificationTargets);
  }

  private java.util.Map<UUID, NotificationRecipientJpaEntity> recipientByNotificationId(
      java.util.Collection<UUID> notificationIds, NotificationActorType actorType, UUID actorId) {
    if (notificationIds == null || notificationIds.isEmpty()) {
      return java.util.Map.of();
    }
    var latestPublicationByNotificationId =
        publications.findByNotificationIdInAndDeletedAtIsNullOrderByPublicationNoDesc(notificationIds)
            .stream()
            .collect(
                Collectors.toMap(
                    NotificationPublicationJpaEntity::getNotificationId,
                    Function.identity(),
                    (existing, ignored) -> existing));
    if (latestPublicationByNotificationId.isEmpty()) {
      return java.util.Map.of();
    }
    var publicationIds =
        latestPublicationByNotificationId.values().stream()
            .map(NotificationPublicationJpaEntity::getId)
            .toList();
    var notificationIdByPublicationId =
        latestPublicationByNotificationId.values().stream()
            .collect(
                Collectors.toMap(
                    NotificationPublicationJpaEntity::getId,
                    NotificationPublicationJpaEntity::getNotificationId));
    return recipients
        .findByPublicationIdInAndRecipientActorTypeAndRecipientActorIdAndDeletedAtIsNull(
            publicationIds, actorType, actorId)
        .stream()
        .collect(
            Collectors.toMap(
                item -> notificationIdByPublicationId.get(item.getPublicationId()),
                Function.identity(),
                (a, b) -> a));
  }

  private String titleText(NotificationJpaEntity entity) {
    return preferredTranslation(entity)
        .map(NotificationTranslationJpaEntity::getTitleText)
        .orElse(entity.getTitleText());
  }

  private String messageText(NotificationJpaEntity entity) {
    return preferredTranslation(entity)
        .map(NotificationTranslationJpaEntity::getBodyText)
        .orElse(entity.getMessageText());
  }

  private Optional<NotificationTranslationJpaEntity> preferredTranslation(NotificationJpaEntity entity) {
    var values = translations.findByNotificationIdAndDeletedAtIsNull(entity.getId());
    return values.stream()
        .filter(value -> "fr".equals(value.getLocale()))
        .findFirst()
        .or(() -> values.stream().findFirst());
  }

  private NotificationAudienceType publicationAudience(NotificationAudienceType audienceType) {
    if (audienceType == null) {
      return NotificationAudienceType.ALL_APP_USERS;
    }
    return switch (audienceType) {
      case SPECIFIC_ACTORS, PLATFORM_ADMINS, ALL_APP_USERS, TENANT_ADMINS, TENANT_APP_USERS,
          TENANT_SELLER_TERMINALS -> audienceType;
    };
  }
}
