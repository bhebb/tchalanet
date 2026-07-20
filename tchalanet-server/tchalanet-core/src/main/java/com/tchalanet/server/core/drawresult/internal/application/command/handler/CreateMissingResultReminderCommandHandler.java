package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.api.command.CreateMissingResultReminderCommand;
import com.tchalanet.server.core.drawresult.api.command.CreateMissingResultReminderResult;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultReminderReason;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultReminderCorrelationKeys;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultReminderExpirationPolicy;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateMissingResultReminderCommandHandler
    implements CommandHandler<
        CreateMissingResultReminderCommand, CreateMissingResultReminderResult> {

  private static final String SOURCE_TYPE = "DRAW_RESULT_ACTION_REQUIRED";

  private final DrawResultReaderPort drawResultReader;
  private final ResultSlotCatalog resultSlotCatalog;
  private final ResultReminderExpirationPolicy expirationPolicy;
  private final NotificationApi notificationApi;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @Override
  @TchTx
  public CreateMissingResultReminderResult handle(CreateMissingResultReminderCommand command) {
    validate(command);

    var dedupeKey =
        ResultReminderCorrelationKeys.actionRequired(
            command.reason(), command.resultSlotId(), command.drawDate());

    var resultId =
        drawResultReader.findByResultSlotIdAndOccurredAt(command.resultSlotId(), command.occurredAt());
    var result = resultId.flatMap(drawResultReader::findViewById).orElse(null);
    if (!isReminderStillRequired(command.reason(), resultId.isPresent(), result)) {
      return new CreateMissingResultReminderResult(dedupeKey, false, true);
    }

    var slot = resultSlotCatalog.findById(command.resultSlotId()).orElse(null);
    var now = Instant.now(clock);
    var expiresAt =
        slot == null ? now.plus(Duration.ofHours(24)) : expirationPolicy.expiresAt(slot, now);
    var label = slot == null ? command.slotKey() : slot.slotKey();
    var provider =
        command.providerCode() == null
            ? (slot == null ? null : slot.provider())
            : command.providerCode();
    var title = title(command.reason(), label, command.drawDate().toString());
    var message =
        message(command.reason(), provider, label, command.occurredAt(), now, command.requestId());
    var translations =
        translations(
            command.reason(),
            label,
            command.drawDate().toString(),
            provider,
            command.occurredAt(),
            now,
            command.requestId());

    notificationApi.createNotification(
        new CreateNotificationRequest(
            null,
            SOURCE_TYPE,
            "%s:%s:%s"
                .formatted(command.resultSlotId().value(), command.drawDate(), command.reason()),
            dedupeKey,
            NotificationAudienceType.PLATFORM_ADMINS,
            Set.of(),
            severity(command.reason()),
            NotificationKind.ACTION_REQUIRED,
            NotificationCategory.RESULT,
            null,
            null,
            title,
            message,
            translations,
            jsonUtils.toJsonNode(payload(command, provider, label, now, dedupeKey)),
            "DRAW_RESULT_ENTRY",
            "/app/platform/ops/draw-results",
            expiresAt,
            Set.of(NotificationChannel.WEB, NotificationChannel.SLACK)));

    return new CreateMissingResultReminderResult(dedupeKey, true, false);
  }

  private static NotificationSeverity severity(ResultReminderReason reason) {
    return reason == ResultReminderReason.AUTOMATIC_FETCH_OVERDUE
            || reason == ResultReminderReason.PROVISIONAL_RESULT_STUCK
        ? NotificationSeverity.WARNING
        : NotificationSeverity.INFO;
  }

  private static String title(ResultReminderReason reason, String label, String drawDate) {
    if (reason == ResultReminderReason.AUTOMATIC_FETCH_OVERDUE) {
      return "Résultat provider non reçu après 1 heure — %s du %s.".formatted(label, drawDate);
    }
    if (reason == ResultReminderReason.PROVISIONAL_RESULT_STUCK) {
      return "Résultat provisoire à confirmer — %s du %s.".formatted(label, drawDate);
    }
    return "Résultat manuel requis — %s du %s.".formatted(label, drawDate);
  }

  private static String message(
      ResultReminderReason reason,
      String provider,
      String label,
      Instant occurredAt,
      Instant now,
      String requestId) {
    var elapsed = Duration.between(occurredAt, now).toMinutes();
    var reasonText =
        switch (reason) {
          case MANUAL_ENTRY_REQUIRED -> "saisie manuelle requise";
          case AUTOMATIC_FETCH_OVERDUE -> "provider automatique en retard";
          case PROVISIONAL_RESULT_STUCK -> "résultat provisoire à confirmer";
        };
    return "Provider=%s, slot=%s, heure attendue=%s, retard=%d min, raison=%s, requestId=%s"
        .formatted(provider, label, occurredAt, Math.max(0, elapsed), reasonText, requestId);
  }

  private static Map<String, NotificationTranslationInput> translations(
      ResultReminderReason reason,
      String label,
      String drawDate,
      String provider,
      Instant occurredAt,
      Instant now,
      String requestId) {
    var elapsed = Math.max(0, Duration.between(occurredAt, now).toMinutes());
    var automatic = reason == ResultReminderReason.AUTOMATIC_FETCH_OVERDUE;
    var provisional = reason == ResultReminderReason.PROVISIONAL_RESULT_STUCK;
    return Map.of(
        "fr",
            new NotificationTranslationInput(
                title(reason, label, drawDate),
                message(reason, provider, label, occurredAt, now, requestId)),
        "en",
            new NotificationTranslationInput(
                automatic
                    ? "Provider result overdue after 1 hour — %s for %s.".formatted(label, drawDate)
                    : provisional
                        ? "Provisional result needs confirmation — %s for %s."
                            .formatted(label, drawDate)
                    : "Manual result required — %s for %s.".formatted(label, drawDate),
                "Provider=%s, slot=%s, expectedAt=%s, overdue=%d min, reason=%s, requestId=%s"
                    .formatted(
                        provider,
                        label,
                        occurredAt,
                        elapsed,
                        automatic
                            ? "automatic provider overdue"
                            : provisional ? "provisional result needs confirmation" : "manual entry required",
                        requestId)),
        "ht",
            new NotificationTranslationInput(
                automatic
                    ? "Rezilta provider an an reta apre 1 èdtan — %s pou %s."
                        .formatted(label, drawDate)
                    : provisional
                        ? "Rezilta pwovizwa pou konfime — %s pou %s.".formatted(label, drawDate)
                    : "Rezilta manyèl obligatwa — %s pou %s.".formatted(label, drawDate),
                "Provider=%s, lè=%s, lè espere=%s, reta=%d min, rezon=%s, requestId=%s"
                    .formatted(
                        provider,
                        label,
                        occurredAt,
                        elapsed,
                        automatic
                            ? "provider otomatik an reta"
                            : provisional ? "rezilta pwovizwa pou konfime" : "antre manyèl obligatwa",
                        requestId)));
  }

  private static boolean isReminderStillRequired(
      ResultReminderReason reason, boolean resultExists, DrawResultView result) {
    if (reason == ResultReminderReason.PROVISIONAL_RESULT_STUCK) {
      return result != null && result.status() == DrawResultStatus.PROVISIONAL;
    }
    return !resultExists;
  }

  private static Map<String, Object> payload(
      CreateMissingResultReminderCommand command,
      String provider,
      String label,
      Instant now,
      String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", SOURCE_TYPE);
    payload.put("reason", command.reason().name());
    payload.put("provider", provider);
    payload.put("slot", label);
    payload.put("resultSlotId", command.resultSlotId().value().toString());
    payload.put("drawDate", command.drawDate().toString());
    payload.put("occurredAt", command.occurredAt().toString());
    payload.put("elapsedMinutes", Duration.between(command.occurredAt(), now).toMinutes());
    payload.put("correlationKey", dedupeKey);
    payload.put("requestId", command.requestId());
    return payload;
  }

  private static void validate(CreateMissingResultReminderCommand command) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(command.resultSlotId(), "resultSlotId is required");
    Objects.requireNonNull(command.drawDate(), "drawDate is required");
    Objects.requireNonNull(command.occurredAt(), "occurredAt is required");
    Objects.requireNonNull(command.reason(), "reason is required");
  }
}
