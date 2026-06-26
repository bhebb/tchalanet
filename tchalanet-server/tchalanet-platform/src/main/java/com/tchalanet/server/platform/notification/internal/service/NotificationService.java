package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationPublicationId;
import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ExpireNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationsReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationUnreadCountView;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Application service for the notification capability. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final Clock clock;
  private final IdGenerator idGenerator;
  private final JsonUtils jsonUtils;
  private final NotificationWriter notificationWriter;
  private final NotificationReader reader;
  private final NotificationTemplateRenderer templateRenderer;
  private final NotificationPolicy notificationPolicy;
  private final com.tchalanet.server.platform.notification.internal.persistence.NotificationTranslationJpaRepository translations;
  private final com.tchalanet.server.platform.notification.internal.persistence.NotificationPublicationJpaRepository publications;
  private final com.tchalanet.server.platform.notification.internal.persistence.NotificationDeliveryPolicyJpaRepository deliveryPolicies;
  private final ApplicationEventPublisher eventPublisher;

  @TchTx
  public void createNotification(CreateNotificationRequest request) {
    if (request.dedupeKey() != null && !request.dedupeKey().isBlank()) {
      var existing = notificationWriter.findByDedupeKey(request.dedupeKey());
      if (existing.isPresent()) {
        return;
      }
    }

    var now = clock.instant();
    var notificationId = NotificationId.of(idGenerator.newUuid());
    var targets = normalizeTargets(request.audienceType(), request.targets());
    validateAudienceScope(request.audienceType(), request.tenantId());
    validateManualTranslations(request);
    var fallbackTranslation = translationFor(request.translations(), "fr");
    var rendered =
        templateRenderer.render(
            request.tenantId(),
            request.titleKey(),
            request.messageKey(),
            null,
            firstNonBlank(request.titleText(), fallbackTranslation == null ? null : fallbackTranslation.title()),
            firstNonBlank(request.messageText(), fallbackTranslation == null ? null : fallbackTranslation.body()),
            request.payload());
    var notification =
        new Notification(
            notificationId,
            request.tenantId(),
            request.sourceType(),
            request.sourceId(),
            request.dedupeKey(),
            request.audienceType(),
            targets,
            request.severity(),
            request.kind(),
            request.category(),
            request.titleKey(),
            request.messageKey(),
            rendered.title(),
            rendered.message(),
            request.payload(),
            new NotificationAction(request.actionType(), request.actionUrl()),
            NotificationStatus.PUBLISHED,
            request.expiresAt(),
            now,
            now);

    var saved = notificationWriter.save(notification);
    saveTranslations(saved.id(), saved.tenantId(), request.translations());
    var publicationId = latestPublicationId(saved.id());
    var deliveryChannels = saveDeliveryPolicies(saved.id(), publicationId, saved.tenantId(), request.channels());
    eventPublisher.publishEvent(
        new NotificationPublishedEvent(
            idGenerator.newUuid(),
            saved.id(),
            publicationId,
            saved.tenantId(),
            saved.audienceType(),
            targets,
            saved.severity(),
            saved.kind(),
            saved.category(),
            saved.titleText(),
            saved.messageText(),
            saved.payload(),
            saved.action() == null ? null : saved.action().url(),
            deliveryChannels,
            now));
    log.debug("Notification created id={} channels={}", saved.id(), deliveryChannels);
  }

  private void validateManualTranslations(CreateNotificationRequest request) {
    var hasTitleKey = request.titleKey() != null && !request.titleKey().isBlank();
    var hasMessageKey = request.messageKey() != null && !request.messageKey().isBlank();
    if (hasTitleKey || hasMessageKey) {
      if (!hasTitleKey || !hasMessageKey) {
        throw ProblemRest.badRequest("notification.system_keys_required");
      }
      return;
    }
    for (String locale : java.util.List.of("fr", "en", "ht")) {
      var translation = translationFor(request.translations(), locale);
      if (translation == null
          || translation.title() == null
          || translation.title().isBlank()
          || translation.body() == null
          || translation.body().isBlank()) {
        throw ProblemRest.badRequest("notification.translation_required_" + locale);
      }
    }
  }

  private void saveTranslations(
      NotificationId notificationId,
      TenantId tenantId,
      Map<String, NotificationTranslationInput> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    values.forEach(
        (locale, value) -> {
          if (!java.util.Set.of("fr", "en", "ht").contains(locale)) {
            throw ProblemRest.badRequest("notification.translation_locale_unsupported");
          }
          if (value == null || value.title() == null || value.title().isBlank()) {
            return;
          }
          var entity =
              new com.tchalanet.server.platform.notification.internal.persistence.NotificationTranslationJpaEntity();
          entity.setNotificationId(notificationId.value());
          entity.setTenantId(tenantId == null ? null : tenantId.value());
          entity.setLocale(locale);
          entity.setTitleText(value.title().trim());
          entity.setBodyText(value.body() == null ? null : value.body().trim());
          translations.save(entity);
        });
  }

  private static NotificationTranslationInput translationFor(
      Map<String, NotificationTranslationInput> values, String locale) {
    return values == null ? null : values.get(locale);
  }

  private NotificationPublicationId latestPublicationId(NotificationId notificationId) {
    return publications
        .findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(notificationId.value())
        .map(entity -> NotificationPublicationId.of(entity.getId()))
        .orElse(null);
  }

  private Set<NotificationDeliveryChannel> saveDeliveryPolicies(
      NotificationId notificationId,
      NotificationPublicationId publicationId,
      TenantId tenantId,
      Set<NotificationChannel> requestedChannels) {
    var channels = normalizeDeliveryChannels(requestedChannels);
    for (var channel : channels) {
      var entity =
          new com.tchalanet.server.platform.notification.internal.persistence.NotificationDeliveryPolicyJpaEntity();
      entity.setNotificationId(notificationId.value());
      entity.setPublicationId(publicationId == null ? null : publicationId.value());
      entity.setTenantId(tenantId == null ? null : tenantId.value());
      entity.setChannel(channel);
      entity.setEnabled(true);
      deliveryPolicies.save(entity);
    }
    return channels;
  }

  private Set<NotificationDeliveryChannel> normalizeDeliveryChannels(Set<NotificationChannel> requestedChannels) {
    if (requestedChannels == null || requestedChannels.isEmpty()) {
      return Set.of(NotificationDeliveryChannel.IN_APP);
    }
    var channels = new java.util.LinkedHashSet<NotificationDeliveryChannel>();
    for (var channel : requestedChannels) {
      if (channel == null) {
        continue;
      }
      switch (channel) {
        case IN_APP, WEB, PUSH -> channels.add(NotificationDeliveryChannel.IN_APP);
        case EMAIL -> channels.add(NotificationDeliveryChannel.EMAIL);
        case SMS -> channels.add(NotificationDeliveryChannel.SMS);
        case WHATSAPP -> channels.add(NotificationDeliveryChannel.WHATSAPP);
        case SLACK -> channels.add(NotificationDeliveryChannel.SLACK);
      }
    }
    return channels.isEmpty() ? Set.of(NotificationDeliveryChannel.IN_APP) : Set.copyOf(channels);
  }

  @TchTx
  public SendNotificationResult sendNotification(SendNotificationRequest request) {
    log.debug(
        "Handling SendNotificationRequest: type={}, severity={}, recipients={}",
        request.type(),
        request.severity(),
        request.recipients().size());

    notificationPolicy.validateRecipients(request.recipients());

    var idempotencyKey =
        request.idempotencyKey() != null
            ? request.idempotencyKey()
            : generateIdempotencyKey(request);

    for (var recipient : request.recipients()) {
      try {
        createInAppNotification(request, recipient, idempotencyKey);
      } catch (Exception e) {
        log.error(
            "Failed to send notification to recipient: channel={}, error={}",
            recipient.channel(),
            e.getMessage(),
            e);
        return SendNotificationResult.failed(
            "Failed to send notification: " + e.getMessage(), idempotencyKey);
      }
    }

    log.info(
        "Notification sent successfully: type={}, idempotencyKey={}",
        request.type(),
        idempotencyKey);

    return SendNotificationResult.accepted(idempotencyKey);
  }

  @TchTx
  public void markRead(MarkNotificationReadRequest request) {
    reader.markRead(request.notificationId(), NotificationActorType.APP_USER, request.actorId().value());
  }

  @TchTx
  public void markReadForTerminal(NotificationId notificationId, SellerTerminalId sellerTerminalId) {
    reader.markRead(notificationId, NotificationActorType.SELLER_TERMINAL, sellerTerminalId.value());
  }

  @TchTx
  public void markRead(MarkNotificationsReadRequest request) {
    request.notificationIds().forEach(id -> reader.markRead(id, NotificationActorType.APP_USER, request.actorId().value()));
  }

  @TchTx
  public void archiveNotification(ArchiveNotificationRequest request) {
    reader.dismiss(request.notificationId(), NotificationActorType.APP_USER, request.actorId().value());
  }

  @TchTx
  public void archiveNotificationForTerminal(
      NotificationId notificationId, SellerTerminalId sellerTerminalId) {
    reader.dismiss(notificationId, NotificationActorType.SELLER_TERMINAL, sellerTerminalId.value());
  }

  @TchTx
  public void archiveNotifications(ArchiveNotificationsRequest request) {
    request.notificationIds().forEach(id -> reader.dismiss(id, NotificationActorType.APP_USER, request.actorId().value()));
  }

  @TchTx
  public Integer expireNotifications(ExpireNotificationsRequest request) {
    var now = request.now() == null ? clock.instant() : request.now();
    return notificationWriter.expire(now);
  }

  public NotificationSummaryView getNotificationSummary(GetNotificationSummaryRequest request) {
    return reader.summary(request.userId(), request.roleCode());
  }

  public NotificationSummaryView getTerminalNotificationSummary(SellerTerminalId sellerTerminalId) {
    return reader.summaryForTerminal(sellerTerminalId);
  }

  public TchPage<NotificationItemView> listNotifications(ListNotificationsRequest request) {
    return reader.list(
        request.userId(),
        request.roleCode(),
        request.status(),
        request.category(),
        request.kind(),
        request.severity(),
        request.search(),
        request.pageRequest());
  }

  public TchPage<NotificationItemView> listTerminalNotifications(
      SellerTerminalId sellerTerminalId,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchPageRequest pageRequest) {
    return reader.listForTerminal(
        sellerTerminalId, status, category, kind, severity, TchSearchQuery.empty(), pageRequest);
  }

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
    return reader.listMyNotifications(
        actorType, actorId, userId, roleCode, status, category, kind, severity, search, pageRequest);
  }

  public NotificationUnreadCountView countUnread(
      NotificationActorType actorType, UUID actorId, UserId userId, String roleCode) {
    return reader.countUnread(actorType, actorId, userId, roleCode);
  }

  @TchTx
  public void markRead(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
    reader.markRead(notificationId, actorType, actorId);
  }

  @TchTx
  public void dismiss(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
    reader.dismiss(notificationId, actorType, actorId);
  }

  @TchTx
  public void markAllRead(NotificationActorType actorType, UUID actorId, UserId userId, String roleCode) {
    reader.markAllRead(actorType, actorId, userId, roleCode);
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }

  private void createInAppNotification(
      SendNotificationRequest request, NotificationRecipient recipient, String idempotencyKey) {
    var now = clock.instant();
    var notificationId = NotificationId.of(idGenerator.newUuid());
    var tenantId = recipient.tenantId();
    var audienceType = audienceType(recipient);
    var targets =
        recipient.userId() == null
            ? java.util.Set.<NotificationTarget>of()
            : java.util.Set.of(NotificationTarget.appUser(recipient.userId().value()));
    var dedupeKey =
        idempotencyKey + ":" + recipient.channel() + ":" + audienceType + ":" + targets.hashCode();

    if (notificationWriter.findByDedupeKey(dedupeKey).isPresent()) {
      return;
    }

    var notification =
        new Notification(
            notificationId,
            tenantId,
            "DOMAIN_EVENT",
            request.type().name(),
            dedupeKey,
            audienceType,
            targets,
            request.severity(),
            NotificationKind.INFO,
            NotificationCategory.SYSTEM,
            request.type().name(),
            request.type().name(),
            request.title(),
            request.message(),
            jsonUtils.toJsonNode(request.context()),
            new NotificationAction(null, null),
            NotificationStatus.PUBLISHED,
            null,
            now,
            now);

    var saved = notificationWriter.save(notification);
    log.debug("In-app notification accepted id={} recipientChannel={}", saved.id(), recipient.channel());
  }

  private NotificationAudienceType audienceType(NotificationRecipient recipient) {
    if (recipient.userId() != null) {
      return NotificationAudienceType.SPECIFIC_ACTORS;
    }
    if (recipient.tenantId() != null) {
      return NotificationAudienceType.TENANT_APP_USERS;
    }
    return NotificationAudienceType.PLATFORM_ADMINS;
  }

  private void validateAudienceScope(NotificationAudienceType audienceType, TenantId tenantId) {
    var normalizedType = audienceType == null ? NotificationAudienceType.TENANT_APP_USERS : audienceType;
    if ((normalizedType == NotificationAudienceType.TENANT_APP_USERS
            || normalizedType == NotificationAudienceType.TENANT_ADMINS
            || normalizedType == NotificationAudienceType.TENANT_SELLER_TERMINALS)
        && tenantId == null) {
      throw ProblemRest.unprocessable("notification.tenant_required");
    }
  }

  private java.util.Set<NotificationTarget> normalizeTargets(
      NotificationAudienceType audienceType,
      java.util.Set<NotificationTarget> targets) {
    var normalizedType = audienceType == null ? NotificationAudienceType.TENANT_APP_USERS : audienceType;
    if (normalizedType != NotificationAudienceType.SPECIFIC_ACTORS) {
      return java.util.Set.of();
    }

    var values = new java.util.LinkedHashSet<NotificationTarget>();
    if (targets != null) {
      targets.stream().filter(java.util.Objects::nonNull).forEach(values::add);
    }
    if (values.isEmpty()) {
      throw ProblemRest.unprocessable("notification.target_required");
    }
    return java.util.Collections.unmodifiableSet(values);
  }

  private String generateIdempotencyKey(SendNotificationRequest request) {
    return request.type().name() + "_" + UUID.randomUUID();
  }
}
