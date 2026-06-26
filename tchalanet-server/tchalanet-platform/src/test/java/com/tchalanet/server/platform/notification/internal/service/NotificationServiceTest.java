package com.tchalanet.server.platform.notification.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationUnreadCountView;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationTranslationJpaEntity;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationTranslationJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

  private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void markReadUsesPersonalActorState() {
    var reader = new RecordingNotificationReader(new NotificationSummaryView(0, 0, 0, false));
    var service = new NotificationService(CLOCK, null, null, null, reader, null, null, null, null, null, null);
    var notificationId = NotificationId.of(UUID.randomUUID());
    var actorId = UserId.of(UUID.randomUUID());

    service.markRead(new MarkNotificationReadRequest(notificationId, actorId));

    assertThat(reader.lastReadId).isEqualTo(notificationId);
    assertThat(reader.lastActorType).isEqualTo(NotificationActorType.APP_USER);
    assertThat(reader.lastActorId).isEqualTo(actorId.value());
  }

  @Test
  void archiveDismissesPersonalActorState() {
    var reader = new RecordingNotificationReader(new NotificationSummaryView(0, 0, 0, false));
    var service = new NotificationService(CLOCK, null, null, null, reader, null, null, null, null, null, null);
    var notificationId = NotificationId.of(UUID.randomUUID());
    var actorId = UserId.of(UUID.randomUUID());

    service.archiveNotification(new ArchiveNotificationRequest(notificationId, actorId));

    assertThat(reader.lastDismissedId).isEqualTo(notificationId);
    assertThat(reader.lastActorType).isEqualTo(NotificationActorType.APP_USER);
    assertThat(reader.lastActorId).isEqualTo(actorId.value());
  }

  @Test
  void summaryReadsUnreadCountsFromReader() {
    var userId = UserId.of(UUID.randomUUID());
    var summary = new NotificationSummaryView(4, 1, 2, true);
    var reader = new RecordingNotificationReader(summary);
    var service = new NotificationService(null, null, null, null, reader, null, null, null, null, null, null);

    var result =
        service.getNotificationSummary(new GetNotificationSummaryRequest(userId, "TENANT_ADMIN"));

    assertThat(result).isEqualTo(summary);
    assertThat(reader.lastUserId).isEqualTo(userId);
    assertThat(reader.lastRoleCode).isEqualTo("TENANT_ADMIN");
  }

  @Test
  void createNotificationRequiresAllManualTranslations() {
    var service = notificationCreationService(new RecordingNotificationWriter(), mock(NotificationTranslationJpaRepository.class));

    assertThatThrownBy(() -> service.createNotification(manualCreateRequest(Map.of())))
        .hasMessageContaining("notification.translation_required_fr");
  }

  @Test
  void createNotificationPersistsManualTranslations() {
    var writer = new RecordingNotificationWriter();
    var savedTranslations = new ArrayList<NotificationTranslationJpaEntity>();
    var translations = mock(NotificationTranslationJpaRepository.class);
    when(translations.save(any(NotificationTranslationJpaEntity.class)))
        .thenAnswer(invocation -> {
          var entity = invocation.getArgument(0, NotificationTranslationJpaEntity.class);
          savedTranslations.add(entity);
          return entity;
        });
    var service = notificationCreationService(writer, translations);

    service.createNotification(
        manualCreateRequest(
            Map.of(
                "fr", new NotificationTranslationInput("Titre", "Message"),
                "en", new NotificationTranslationInput("Title", "Message"),
                "ht", new NotificationTranslationInput("Tit", "Mesaj"))));

    assertThat(writer.saved).isNotNull();
    assertThat(savedTranslations).hasSize(3);
    assertThat(savedTranslations).extracting(NotificationTranslationJpaEntity::getLocale)
        .containsExactlyInAnyOrder("fr", "en", "ht");
  }

  private static NotificationService notificationCreationService(
      NotificationWriter writer, NotificationTranslationJpaRepository translations) {
    var idGenerator = mock(IdGenerator.class);
    when(idGenerator.newUuid()).thenReturn(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    var renderer = mock(NotificationTemplateRenderer.class);
    when(renderer.render(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new RenderedNotification("Titre", "Message"));
    var publications =
        mock(com.tchalanet.server.platform.notification.internal.persistence.NotificationPublicationJpaRepository.class);
    when(publications.findFirstByNotificationIdAndDeletedAtIsNullOrderByPublicationNoDesc(any()))
        .thenReturn(Optional.empty());
    return new NotificationService(
        CLOCK,
        idGenerator,
        null,
        writer,
        null,
        renderer,
        null,
        translations,
        publications,
        mock(com.tchalanet.server.platform.notification.internal.persistence.NotificationDeliveryPolicyJpaRepository.class),
        mock(org.springframework.context.ApplicationEventPublisher.class));
  }

  private static CreateNotificationRequest manualCreateRequest(
      Map<String, NotificationTranslationInput> translations) {
    return new CreateNotificationRequest(
        null,
        "SUPER_ADMIN",
        null,
        null,
        NotificationAudienceType.PLATFORM_ADMINS,
        java.util.Set.of(),
        NotificationSeverity.INFO,
        NotificationKind.INFO,
        NotificationCategory.SYSTEM,
        null,
        null,
        null,
        null,
        translations,
        null,
        null,
        null,
        null,
        java.util.Set.of());
  }

  private static final class RecordingNotificationWriter implements NotificationWriter {
    private Notification saved;

    @Override
    public Optional<Notification> findByDedupeKey(String dedupeKey) {
      return Optional.empty();
    }

    @Override
    public Notification save(Notification notification) {
      this.saved = notification;
      return notification;
    }

    @Override
    public int expire(Instant now) {
      return 0;
    }
  }

  private static final class RecordingNotificationReader implements NotificationReader {
    private final NotificationSummaryView summary;
    private UserId lastUserId;
    private String lastRoleCode;
    private NotificationId lastReadId;
    private NotificationId lastDismissedId;
    private NotificationActorType lastActorType;
    private UUID lastActorId;

    private RecordingNotificationReader(NotificationSummaryView summary) {
      this.summary = summary;
    }

    @Override
    public NotificationSummaryView summary(UserId userId, String roleCode) {
      this.lastUserId = userId;
      this.lastRoleCode = roleCode;
      return summary;
    }

    @Override
    public NotificationSummaryView summaryForTerminal(SellerTerminalId sellerTerminalId) {
      throw new UnsupportedOperationException("Not needed by this test");
    }

    @Override
    public TchPage<NotificationItemView> list(
        UserId userId,
        String roleCode,
        Optional<NotificationStatus> status,
        Optional<NotificationCategory> category,
        Optional<NotificationKind> kind,
        Optional<NotificationSeverity> severity,
        TchPageRequest pageRequest) {
      throw new UnsupportedOperationException("Not needed by this test");
    }

    @Override
    public TchPage<NotificationItemView> listForTerminal(
        SellerTerminalId sellerTerminalId,
        Optional<NotificationStatus> status,
        Optional<NotificationCategory> category,
        Optional<NotificationKind> kind,
        Optional<NotificationSeverity> severity,
        TchPageRequest pageRequest) {
      throw new UnsupportedOperationException("Not needed by this test");
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
        TchPageRequest pageRequest) {
      throw new UnsupportedOperationException("Not needed by this test");
    }

    @Override
    public NotificationUnreadCountView countUnread(
        NotificationActorType actorType, UUID actorId, UserId userId, String roleCode) {
      throw new UnsupportedOperationException("Not needed by this test");
    }

    @Override
    public void markRead(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
      this.lastReadId = notificationId;
      this.lastActorType = actorType;
      this.lastActorId = actorId;
    }

    @Override
    public void dismiss(NotificationId notificationId, NotificationActorType actorType, UUID actorId) {
      this.lastDismissedId = notificationId;
      this.lastActorType = actorType;
      this.lastActorId = actorId;
    }

    @Override
    public void markAllRead(NotificationActorType actorType, UUID actorId, UserId userId, String roleCode) {
      throw new UnsupportedOperationException("Not needed by this test");
    }
  }
}
