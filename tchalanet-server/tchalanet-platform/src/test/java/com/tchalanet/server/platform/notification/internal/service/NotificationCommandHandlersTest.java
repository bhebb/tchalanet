package com.tchalanet.server.platform.notification.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.notification.api.model.ArchiveNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.GetNotificationSummaryQuery;
import com.tchalanet.server.platform.notification.api.model.MarkNotificationReadCommand;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryView;
import com.tchalanet.server.platform.notification.api.model.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationSummaryView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationCommandHandlersTest {

  private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void markReadUsesCurrentClockInstant() {
    var writer = new RecordingNotificationWriter();
    var handler = new MarkNotificationReadCommandHandler(CLOCK, writer);
    var notificationId = NotificationId.of(UUID.randomUUID());
    var actorId = UserId.of(UUID.randomUUID());

    handler.handle(new MarkNotificationReadCommand(notificationId, actorId));

    assertThat(writer.lastReadId).isEqualTo(notificationId);
    assertThat(writer.lastReadAt).isEqualTo(NOW);
  }

  @Test
  void archiveUsesCurrentClockInstant() {
    var writer = new RecordingNotificationWriter();
    var handler = new ArchiveNotificationCommandHandler(CLOCK, writer);
    var notificationId = NotificationId.of(UUID.randomUUID());
    var actorId = UserId.of(UUID.randomUUID());

    handler.handle(new ArchiveNotificationCommand(notificationId, actorId));

    assertThat(writer.lastArchivedId).isEqualTo(notificationId);
    assertThat(writer.lastArchivedAt).isEqualTo(NOW);
  }

  @Test
  void summaryReadsUnreadCountsFromReader() {
    var userId = UserId.of(UUID.randomUUID());
    var summary = new NotificationSummaryView(4, 1, 2, true);
    var reader = new RecordingNotificationReader(summary);
    var handler = new GetNotificationSummaryQueryHandler(reader);

    var result = handler.handle(new GetNotificationSummaryQuery(userId, "TENANT_ADMIN"));

    assertThat(result).isEqualTo(summary);
    assertThat(reader.lastUserId).isEqualTo(userId);
    assertThat(reader.lastRoleCode).isEqualTo("TENANT_ADMIN");
  }

  private static final class RecordingNotificationWriter implements NotificationWriterPort {
    private NotificationId lastReadId;
    private Instant lastReadAt;
    private NotificationId lastArchivedId;
    private Instant lastArchivedAt;

    @Override
    public Optional<Notification> findByDedupeKey(String dedupeKey) {
      return Optional.empty();
    }

    @Override
    public Notification save(Notification notification) {
      return notification;
    }

    @Override
    public void markRead(NotificationId id, Instant readAt) {
      this.lastReadId = id;
      this.lastReadAt = readAt;
    }

    @Override
    public void markRead(List<NotificationId> ids, Instant readAt) {}

    @Override
    public void archive(NotificationId id, Instant archivedAt) {
      this.lastArchivedId = id;
      this.lastArchivedAt = archivedAt;
    }

    @Override
    public void archive(List<NotificationId> ids, Instant archivedAt) {}

    @Override
    public int expire(Instant now) {
      return 0;
    }
  }

  private static final class RecordingNotificationReader implements NotificationReaderPort {
    private final NotificationSummaryView summary;
    private UserId lastUserId;
    private String lastRoleCode;

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
    public TchPage<NotificationDeliveryView> listDeliveries(
        Optional<UUID> notificationId,
        Optional<NotificationDeliveryStatus> status,
        TchPageRequest pageRequest) {
      throw new UnsupportedOperationException("Not needed by this test");
    }
  }
}
