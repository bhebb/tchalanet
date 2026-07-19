package com.tchalanet.server.core.drawresult.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.DrawResultAffectedTenantApi;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.api.query.DrawResultAffectedTenant;
import com.tchalanet.server.core.drawresult.api.command.MarkDrawResultOverriddenCommand;
import com.tchalanet.server.core.drawresult.api.event.GlobalDrawResultAvailableEvent;
import com.tchalanet.server.core.drawresult.api.event.GlobalDrawResultCorrectedEvent;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultReminderCorrelationKeys;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ExpireNotificationsByDedupeKeysRequest;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener pour les événements liés aux DrawResults.
 *
 * <p>Gère la mise à jour automatique du status des DrawResults suite à des événements du module
 * draw.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultEventListener {

  private static final String KEY_MARK_OVERRIDDEN = "drawresult.mark_overridden";
  private static final String KEY_RESOLVE_AVAILABLE_REMINDERS = "drawresult.resolve_available";
  private static final String KEY_RESOLVE_CORRECTED_REMINDERS = "drawresult.resolve_corrected";

  private final ProcessedEventPort processedEventPort;
  private final CommandBus commandBus;
  private final Clock clock;
  private final NotificationApi notificationApi;
  private final DrawResultAffectedTenantApi affectedTenantApi;
  private final JsonUtils jsonUtils;

  /**
   * Écoute l'événement DrawResultCorrectedEvent et déclenche le marquage du previous DrawResult
   * comme OVERRIDDEN.
   *
   * <p>Étape 9 du flow de correction de résultat.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
    // Idempotency check
    if (!processedEventPort.markProcessedIfAbsent(KEY_MARK_OVERRIDDEN, event.eventId().value())) {
      log.debug("DrawResultCorrectedEvent already processed: eventId={}", event.eventId());
      return;
    }

    // Délègue au handler via CommandBus
    commandBus.execute(
        new MarkDrawResultOverriddenCommand(
            event.previousDrawResultId(), event.reason(), clock.instant()));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGlobalDrawResultAvailable(GlobalDrawResultAvailableEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_RESOLVE_AVAILABLE_REMINDERS, event.eventId().value())) {
      return;
    }
    resolveActionRequiredReminders(event.resultSlotId(), event.drawDate());
    createTenantResultAvailableNotifications(event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGlobalDrawResultCorrected(GlobalDrawResultCorrectedEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_RESOLVE_CORRECTED_REMINDERS, event.eventId().value())) {
      return;
    }
    resolveActionRequiredReminders(event.resultSlotId(), event.drawDate());
    createOpsResultCorrectedNotification(event);
    createTenantResultCorrectedNotifications(event);
  }

  private void resolveActionRequiredReminders(
      com.tchalanet.server.common.types.id.ResultSlotId resultSlotId,
      java.time.LocalDate drawDate) {
    var now = clock.instant();
    var dedupeKeys =
        List.of(
            ResultReminderCorrelationKeys.manual(resultSlotId, drawDate),
            ResultReminderCorrelationKeys.automaticOverdue(resultSlotId, drawDate));
    var resolved =
        notificationApi.expireByDedupeKeys(
            new ExpireNotificationsByDedupeKeysRequest(dedupeKeys, now));
    log.debug(
        "drawresult.reminder.resolve resultSlotId={} drawDate={} resolved={}",
        resultSlotId,
        drawDate,
        resolved);
  }

  private void createTenantResultAvailableNotifications(GlobalDrawResultAvailableEvent event) {
    var affectedTenants =
        affectedTenantApi.listAffectedTenants(event.resultSlotId(), event.drawDate());

    for (var affected : affectedTenants) {
      var dedupeKey = resultAvailableDedupeKey(event, affected.tenantId());
      var title = "Résultat disponible";
      var message =
          "Résultat disponible pour %s du %s."
              .formatted(displayName(affected, event.resultSlotKey()), event.drawDate());
      var display = displayName(affected, event.resultSlotKey());

      notificationApi.createNotification(
          new CreateNotificationRequest(
              affected.tenantId(),
              "DRAW_RESULT_AVAILABLE",
              event.drawResultId().value().toString(),
              dedupeKey,
              NotificationAudienceType.TENANT_ADMINS,
              Set.of(),
              NotificationSeverity.INFO,
              NotificationKind.INFO,
              NotificationCategory.RESULT,
              null,
              null,
              title,
              message,
              resultAvailableTranslations(display, event.drawDate().toString()),
              jsonUtils.toJsonNode(resultAvailablePayload(event, affected, dedupeKey)),
              "DRAW_RESULT_VIEW",
              "/app/admin/draw-results",
              null,
              Set.of(NotificationChannel.WEB)));
    }

    log.debug(
        "drawresult.available.notifications resultSlotId={} drawDate={} tenants={}",
        event.resultSlotId(),
        event.drawDate(),
        affectedTenants.size());
  }

  private static String resultAvailableDedupeKey(
      GlobalDrawResultAvailableEvent event, TenantId tenantId) {
    return "drawresult.available:%s:%s:%s"
        .formatted(event.drawResultId().value(), tenantId.value(), "TENANT_ADMINS");
  }

  private static String displayName(DrawResultAffectedTenant affected, String fallbackSlotKey) {
    if (affected.drawChannelLabel() != null && !affected.drawChannelLabel().isBlank()) {
      return affected.drawChannelLabel();
    }
    if (affected.drawChannelCode() != null && !affected.drawChannelCode().isBlank()) {
      return affected.drawChannelCode();
    }
    return fallbackSlotKey;
  }

  private static Map<String, Object> resultAvailablePayload(
      GlobalDrawResultAvailableEvent event, DrawResultAffectedTenant affected, String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "DRAW_RESULT_AVAILABLE");
    payload.put("drawResultId", event.drawResultId().value().toString());
    payload.put("resultSlotId", event.resultSlotId().value().toString());
    payload.put("resultSlotKey", event.resultSlotKey());
    payload.put("providerCode", event.providerCode());
    payload.put("source", event.source().name());
    payload.put("drawDate", event.drawDate().toString());
    payload.put("drawId", affected.drawId().value().toString());
    payload.put("drawChannelId", affected.drawChannelId().value().toString());
    payload.put("drawChannelCode", affected.drawChannelCode());
    payload.put("drawChannelLabel", affected.drawChannelLabel());
    payload.put("audienceType", "TENANT_ADMINS");
    payload.put("correlationKey", dedupeKey);
    return payload;
  }

  private void createOpsResultCorrectedNotification(GlobalDrawResultCorrectedEvent event) {
    var dedupeKey =
        "drawresult.corrected:%s:%s".formatted(event.drawResultId().value(), "PLATFORM_ADMINS");
    var title = "Résultat corrigé";
    var message =
        "Résultat corrigé pour %s du %s. Source=%s, reason=%s"
            .formatted(event.resultSlotKey(), event.drawDate(), event.source(), event.reason());

    notificationApi.createNotification(
        new CreateNotificationRequest(
            null,
            "DRAW_RESULT_CORRECTED",
            event.drawResultId().value().toString(),
            dedupeKey,
            NotificationAudienceType.PLATFORM_ADMINS,
            Set.of(),
            NotificationSeverity.WARNING,
            NotificationKind.ACTION_REQUIRED,
            NotificationCategory.RESULT,
            null,
            null,
            title,
            message,
            resultCorrectedOpsTranslations(
                event.resultSlotKey(),
                event.drawDate().toString(),
                event.source().name(),
                event.reason()),
            jsonUtils.toJsonNode(resultCorrectedPayload(event, dedupeKey)),
            "DRAW_RESULT_VIEW",
            "/app/platform/ops/draw-results",
            null,
            Set.of(NotificationChannel.WEB)));
  }

  private static Map<String, Object> resultCorrectedPayload(
      GlobalDrawResultCorrectedEvent event, String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "DRAW_RESULT_CORRECTED");
    payload.put("drawResultId", event.drawResultId().value().toString());
    payload.put("resultSlotId", event.resultSlotId().value().toString());
    payload.put("resultSlotKey", event.resultSlotKey());
    payload.put("providerCode", event.providerCode());
    payload.put("source", event.source().name());
    payload.put("drawDate", event.drawDate().toString());
    payload.put("reason", event.reason());
    payload.put("audienceType", "PLATFORM_ADMINS");
    payload.put("correlationKey", dedupeKey);
    return payload;
  }

  private void createTenantResultCorrectedNotifications(GlobalDrawResultCorrectedEvent event) {
    var affectedTenants =
        affectedTenantApi.listAffectedTenants(event.resultSlotId(), event.drawDate());

    for (var affected : affectedTenants) {
      var dedupeKey = resultCorrectedTenantDedupeKey(event, affected.tenantId());
      var message =
          "Résultat corrigé pour %s du %s."
              .formatted(displayName(affected, event.resultSlotKey()), event.drawDate());
      var display = displayName(affected, event.resultSlotKey());

      notificationApi.createNotification(
          new CreateNotificationRequest(
              affected.tenantId(),
              "DRAW_RESULT_CORRECTED",
              event.drawResultId().value().toString(),
              dedupeKey,
              NotificationAudienceType.TENANT_ADMINS,
              Set.of(),
              NotificationSeverity.WARNING,
              NotificationKind.INFO,
              NotificationCategory.RESULT,
              null,
              null,
              "Résultat corrigé",
              message,
              resultCorrectedTenantTranslations(display, event.drawDate().toString()),
              jsonUtils.toJsonNode(resultCorrectedTenantPayload(event, affected, dedupeKey)),
              "DRAW_RESULT_VIEW",
              "/app/admin/draw-results",
              null,
              Set.of(NotificationChannel.WEB)));
    }

    log.debug(
        "drawresult.corrected.notifications resultSlotId={} drawDate={} tenants={}",
        event.resultSlotId(),
        event.drawDate(),
        affectedTenants.size());
  }

  private static Map<String, NotificationTranslationInput> resultAvailableTranslations(
      String display, String drawDate) {
    return Map.of(
        "fr",
            new NotificationTranslationInput(
                "Résultat disponible",
                "Résultat disponible pour %s du %s.".formatted(display, drawDate)),
        "en",
            new NotificationTranslationInput(
                "Result available", "Result available for %s on %s.".formatted(display, drawDate)),
        "ht",
            new NotificationTranslationInput(
                "Rezilta disponib",
                "Rezilta disponib pou %s jou %s.".formatted(display, drawDate)));
  }

  private static Map<String, NotificationTranslationInput> resultCorrectedOpsTranslations(
      String slotKey, String drawDate, String source, String reason) {
    return Map.of(
        "fr",
            new NotificationTranslationInput(
                "Résultat corrigé",
                "Résultat corrigé pour %s du %s. Source=%s, reason=%s"
                    .formatted(slotKey, drawDate, source, reason)),
        "en",
            new NotificationTranslationInput(
                "Result corrected",
                "Result corrected for %s on %s. Source=%s, reason=%s"
                    .formatted(slotKey, drawDate, source, reason)),
        "ht",
            new NotificationTranslationInput(
                "Rezilta korije",
                "Rezilta korije pou %s jou %s. Sous=%s, rezon=%s"
                    .formatted(slotKey, drawDate, source, reason)));
  }

  private static Map<String, NotificationTranslationInput> resultCorrectedTenantTranslations(
      String display, String drawDate) {
    return Map.of(
        "fr",
            new NotificationTranslationInput(
                "Résultat corrigé", "Résultat corrigé pour %s du %s.".formatted(display, drawDate)),
        "en",
            new NotificationTranslationInput(
                "Result corrected", "Result corrected for %s on %s.".formatted(display, drawDate)),
        "ht",
            new NotificationTranslationInput(
                "Rezilta korije", "Rezilta korije pou %s jou %s.".formatted(display, drawDate)));
  }

  private static String resultCorrectedTenantDedupeKey(
      GlobalDrawResultCorrectedEvent event, TenantId tenantId) {
    return "drawresult.corrected:%s:%s:%s"
        .formatted(event.drawResultId().value(), tenantId.value(), "TENANT_ADMINS");
  }

  private static Map<String, Object> resultCorrectedTenantPayload(
      GlobalDrawResultCorrectedEvent event, DrawResultAffectedTenant affected, String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "DRAW_RESULT_CORRECTED");
    payload.put("drawResultId", event.drawResultId().value().toString());
    payload.put("resultSlotId", event.resultSlotId().value().toString());
    payload.put("resultSlotKey", event.resultSlotKey());
    payload.put("providerCode", event.providerCode());
    payload.put("source", event.source().name());
    payload.put("drawDate", event.drawDate().toString());
    payload.put("reason", event.reason());
    payload.put("drawId", affected.drawId().value().toString());
    payload.put("drawChannelId", affected.drawChannelId().value().toString());
    payload.put("drawChannelCode", affected.drawChannelCode());
    payload.put("drawChannelLabel", affected.drawChannelLabel());
    payload.put("audienceType", "TENANT_ADMINS");
    payload.put("correlationKey", dedupeKey);
    return payload;
  }
}
