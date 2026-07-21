package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.api.command.CreateMissingResultReminderCommand;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.api.model.ResultReminderReason;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultReminderExpirationPolicy;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateMissingResultReminderCommandHandlerTest {

  private static final Instant NOW = Instant.parse("2026-07-16T22:00:00Z");
  private static final LocalDate DRAW_DATE = LocalDate.of(2026, 7, 16);
  private static final Instant OCCURRED_AT = Instant.parse("2026-07-16T21:00:00Z");

  @Test
  void createsManualReminderWithWebAndSlackChannelsWhenResultIsAbsent() {
    var fixture = fixture(Optional.empty());

    var result =
        fixture.handler.handle(
            command(ResultReminderReason.MANUAL_ENTRY_REQUIRED, fixture.resultSlotId));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(fixture.notificationApi).createNotification(captor.capture());

    var request = captor.getValue();
    assertThat(result.created()).isTrue();
    assertThat(result.skippedResultAlreadyPresent()).isFalse();
    assertThat(request.audienceType()).isEqualTo(NotificationAudienceType.PLATFORM_ADMINS);
    assertThat(request.channels())
        .containsExactlyInAnyOrder(NotificationChannel.WEB, NotificationChannel.SLACK);
    assertThat(request.dedupeKey())
        .isEqualTo(
            "drawresult.action-required:manual:%s:%s"
                .formatted(fixture.resultSlotId.value(), DRAW_DATE));
    assertThat(request.translations()).containsOnlyKeys("fr", "en", "ht");
    assertThat(request.translations().get("fr").title()).contains("Résultat manuel requis");
    assertThat(request.translations().get("en").body()).contains("manual entry required");
  }

  @Test
  void createsAutomaticOverdueReminderWithAutomaticDedupeKey() {
    var fixture = fixture(Optional.empty());

    fixture.handler.handle(
        command(ResultReminderReason.AUTOMATIC_FETCH_OVERDUE, fixture.resultSlotId));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(fixture.notificationApi).createNotification(captor.capture());

    var request = captor.getValue();
    assertThat(request.dedupeKey())
        .isEqualTo(
            "drawresult.action-required:automatic-overdue:%s:%s"
                .formatted(fixture.resultSlotId.value(), DRAW_DATE));
    assertThat(request.translations()).containsOnlyKeys("fr", "en", "ht");
    assertThat(request.translations().get("en").title()).contains("Provider result overdue");
  }

  @Test
  void skipsReminderWhenResultExistsBeforeHandling() {
    var fixture = fixture(Optional.of(DrawResultId.of(UUID.randomUUID())));

    var result =
        fixture.handler.handle(
            command(ResultReminderReason.MANUAL_ENTRY_REQUIRED, fixture.resultSlotId));

    assertThat(result.created()).isFalse();
    assertThat(result.skippedResultAlreadyPresent()).isTrue();
    verify(fixture.notificationApi, never()).createNotification(any());
  }

  @Test
  void createsReminderWhenExistingResultRemainsProvisional() {
    var resultId = DrawResultId.of(UUID.randomUUID());
    var fixture = fixture(Optional.of(resultId));
    when(fixture.reader.findViewById(resultId))
        .thenReturn(
            Optional.of(
                new DrawResultView(
                    resultId,
                    "MN_EVE",
                    DRAW_DATE,
                    OCCURRED_AT,
                    DrawResultStatus.PROVISIONAL,
                    DrawSource.EXTERNAL,
                    ResultQuality.COMPLETE,
                    null,
                    NOW,
                    null,
                    null,
                    null,
                    null)));

    var result =
        fixture.handler.handle(
            command(ResultReminderReason.PROVISIONAL_RESULT_STUCK, fixture.resultSlotId));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(fixture.notificationApi).createNotification(captor.capture());
    assertThat(result.created()).isTrue();
    assertThat(captor.getValue().dedupeKey())
        .isEqualTo(
            "drawresult.action-required:provisional-stuck:%s:%s"
                .formatted(fixture.resultSlotId.value(), DRAW_DATE));
  }

  private static Fixture fixture(Optional<DrawResultId> existingResult) {
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var reader = mock(DrawResultReaderPort.class);
    when(reader.findByResultSlotIdAndOccurredAt(resultSlotId, OCCURRED_AT))
        .thenReturn(existingResult);

    var slot = slot(resultSlotId);
    var catalog = mock(ResultSlotCatalog.class);
    when(catalog.findById(resultSlotId)).thenReturn(Optional.of(slot));

    var expiration = mock(ResultReminderExpirationPolicy.class);
    when(expiration.expiresAt(slot, NOW)).thenReturn(NOW.plusSeconds(3600));

    var notificationApi = mock(NotificationApi.class);
    var jsonUtils = mock(JsonUtils.class);
    when(jsonUtils.toJsonNode(any())).thenReturn(JsonUtils.emptyObject());

    return new Fixture(
        resultSlotId,
        reader,
        notificationApi,
        new CreateMissingResultReminderCommandHandler(
            reader,
            catalog,
            expiration,
            notificationApi,
            jsonUtils,
            Clock.fixed(NOW, ZoneOffset.UTC)));
  }

  private static CreateMissingResultReminderCommand command(
      ResultReminderReason reason, ResultSlotId resultSlotId) {
    return new CreateMissingResultReminderCommand(
        resultSlotId, DRAW_DATE, OCCURRED_AT, reason, "MN", "MN_EVE", "req-1");
  }

  private static ResultSlotView slot(ResultSlotId resultSlotId) {
    return new ResultSlotView(
        resultSlotId,
        "MN_EVE",
        "MN",
        ZoneId.of("America/New_York"),
        LocalTime.of(17, 0),
        "MON-SUN",
        true,
        JsonUtils.emptyObject(),
        JsonUtils.emptyObject(),
        "slot.mn.eve");
  }

  private record Fixture(
      ResultSlotId resultSlotId,
      DrawResultReaderPort reader,
      NotificationApi notificationApi,
      CreateMissingResultReminderCommandHandler handler) {}
}
