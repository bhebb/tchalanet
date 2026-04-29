package com.tchalanet.server.core.notification.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationCommand;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationsCommand;
import com.tchalanet.server.core.notification.application.command.model.CreateNotificationCommand;
import com.tchalanet.server.core.notification.application.command.model.MarkNotificationReadCommand;
import com.tchalanet.server.core.notification.application.command.model.MarkNotificationsReadCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationDeliveryWriterPort;
import com.tchalanet.server.core.notification.application.port.out.NotificationWriterPort;
import com.tchalanet.server.core.notification.domain.model.Notification;
import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationDelivery;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateNotificationHandlerTest {

  private static final Instant NOW = Instant.parse("2026-04-29T12:00:00Z");
  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @Test
  void shouldReuseExistingNotificationWhenDedupeKeyAlreadyExists() {
    var existingId = NotificationId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    var writer = new FakeNotificationWriter(existingNotification(existingId, "same-key"));
    var deliveries = new FakeDeliveryWriter();
    var handler =
        new CreateNotificationHandler(fixedClock(), new FixedIdGenerator(), writer, deliveries);

    handler.handle(command("same-key"));

    assertThat(writer.saved).isEmpty();
    assertThat(deliveries.saved).isEmpty();
  }

  @Test
  void shouldCreateWebDeliveryWhenNotificationIsNew() {
    var writer = new FakeNotificationWriter(null);
    var deliveries = new FakeDeliveryWriter();
    var handler =
        new CreateNotificationHandler(fixedClock(), new FixedIdGenerator(), writer, deliveries);

    handler.handle(command("new-key"));

    assertThat(writer.saved).hasSize(1);
    assertThat(writer.saved.getFirst().id().value())
        .isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    assertThat(writer.saved.getFirst().status().name()).isEqualTo("UNREAD");
    assertThat(deliveries.saved).hasSize(1);
    assertThat(deliveries.saved.getFirst().channel()).isEqualTo(NotificationChannel.WEB);
  }

  @Test
  void shouldDelegateReadAndArchiveLifecycleToWriter() {
    var writer = new FakeNotificationWriter(null);

    new MarkNotificationReadHandler(fixedClock(), writer)
        .handle(new MarkNotificationReadCommand(NotificationId.of(UUID.randomUUID()), null));
    new ArchiveNotificationHandler(fixedClock(), writer)
        .handle(new ArchiveNotificationCommand(NotificationId.of(UUID.randomUUID()), null));
    new MarkNotificationsReadHandler(fixedClock(), writer)
        .handle(
            new MarkNotificationsReadCommand(
                List.of(NotificationId.of(UUID.randomUUID()), NotificationId.of(UUID.randomUUID())),
                null));
    new ArchiveNotificationsHandler(fixedClock(), writer)
        .handle(
            new ArchiveNotificationsCommand(
                List.of(NotificationId.of(UUID.randomUUID()), NotificationId.of(UUID.randomUUID())),
                null));

    assertThat(writer.readAt).isEqualTo(NOW);
    assertThat(writer.archivedAt).isEqualTo(NOW);
    assertThat(writer.bulkReadAt).isEqualTo(NOW);
    assertThat(writer.bulkArchivedAt).isEqualTo(NOW);
    assertThat(writer.bulkReadIds).hasSize(2);
    assertThat(writer.bulkArchivedIds).hasSize(2);
  }

  private static CreateNotificationCommand command(String dedupeKey) {
    return new CreateNotificationCommand(
        TENANT_ID,
        "TEST",
        "source-1",
        dedupeKey,
        NotificationAudienceType.ROLE,
        "TENANT_ADMIN",
        NotificationSeverity.WARNING,
        NotificationKind.ACTION_REQUIRED,
        NotificationCategory.PAGE_MODEL,
        "notifications.test.title",
        "notifications.test.message",
        null,
        null,
        null,
        "TEST_ACTION",
        "/admin/test",
        null,
        Set.of(NotificationChannel.WEB));
  }

  private static Notification existingNotification(NotificationId id, String dedupeKey) {
    return new Notification(
        id,
        TENANT_ID,
        "TEST",
        "source-1",
        dedupeKey,
        NotificationAudienceType.ROLE,
        "TENANT_ADMIN",
        NotificationSeverity.WARNING,
        NotificationKind.ACTION_REQUIRED,
        NotificationCategory.PAGE_MODEL,
        "notifications.test.title",
        "notifications.test.message",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        NOW,
        NOW);
  }

  private static Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  private static final class FixedIdGenerator implements IdGenerator {
    private int calls;

    @Override
    public UUID newUuid() {
      calls++;
      return calls == 1
          ? UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
          : UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    }
  }

  private static final class FakeNotificationWriter implements NotificationWriterPort {
    private final Notification existing;
    private final ArrayList<Notification> saved = new ArrayList<>();
    private Instant readAt;
    private Instant archivedAt;
    private Instant bulkReadAt;
    private Instant bulkArchivedAt;
    private List<NotificationId> bulkReadIds = List.of();
    private List<NotificationId> bulkArchivedIds = List.of();

    private FakeNotificationWriter(Notification existing) {
      this.existing = existing;
    }

    @Override
    public Optional<Notification> findByDedupeKey(String dedupeKey) {
      return Optional.ofNullable(existing).filter(n -> n.dedupeKey().equals(dedupeKey));
    }

    @Override
    public Notification save(Notification notification) {
      saved.add(notification);
      return notification;
    }

    @Override
    public void markRead(NotificationId id, Instant readAt) {
      this.readAt = readAt;
    }

    @Override
    public void markRead(List<NotificationId> ids, Instant readAt) {
      this.bulkReadIds = ids;
      this.bulkReadAt = readAt;
    }

    @Override
    public void archive(NotificationId id, Instant archivedAt) {
      this.archivedAt = archivedAt;
    }

    @Override
    public void archive(List<NotificationId> ids, Instant archivedAt) {
      this.bulkArchivedIds = ids;
      this.bulkArchivedAt = archivedAt;
    }

    @Override
    public int expire(Instant now) {
      return 0;
    }
  }

  private static final class FakeDeliveryWriter implements NotificationDeliveryWriterPort {
    private final ArrayList<NotificationDelivery> saved = new ArrayList<>();

    @Override
    public NotificationDelivery save(NotificationDelivery delivery) {
      saved.add(delivery);
      return delivery;
    }
  }
}
