package com.tchalanet.server.core.draw.internal.infra.event;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettlementAttentionEvent;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/** Creates and resolves the one platform attention notice for a delayed draw settlement. */
@Component
@RequiredArgsConstructor
public class DrawSettlementAttentionNotificationListener {

  private static final String HANDLER_KEY = "draw.settlement.attention";

  private final ProcessedEventPort processedEvents;
  private final NotificationApi notificationApi;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onSettlementAttention(DrawSettlementAttentionEvent event) {
    if (!processedEvents.markProcessedIfAbsent(HANDLER_KEY, event.eventId().value())) {
      return;
    }

    var dedupeKey = dedupeKey(event.drawId().value().toString(), event.drawResultId().value().toString());
    var title = "Règlement de tirage à vérifier";
    var message =
        "%d ticket(s) restent à traiter pour le tirage du %s; échecs du dernier essai: %d."
            .formatted(event.pendingTickets(), event.drawDate(), event.failedTickets());

    notificationApi.createNotification(
        new CreateNotificationRequest(
            null,
            "DRAW_SETTLEMENT_ATTENTION_REQUIRED",
            event.drawId().value().toString(),
            dedupeKey,
            NotificationAudienceType.PLATFORM_ADMINS,
            Set.of(),
            NotificationSeverity.ERROR,
            NotificationKind.ACTION_REQUIRED,
            NotificationCategory.DRAW,
            null,
            null,
            title,
            message,
            translations(event),
            jsonUtils.toJsonNode(payload(event, dedupeKey)),
            "DRAW_SETTLEMENT_VIEW",
            "/app/platform/ops/draws/"
                + event.tenantId().value()
                + "/"
                + event.drawId().value(),
            null,
            Set.of(NotificationChannel.WEB, NotificationChannel.SLACK)));
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawSettled(DrawSettledEvent event) {
    expire(event.drawId().value().toString(), event.drawResultId().value().toString());
  }

  @EventListener
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
    expire(event.drawId().value().toString(), event.previousDrawResultId().value().toString());
  }

  static String dedupeKey(String drawId, String drawResultId) {
    return "draw.settlement.attention:%s:%s".formatted(drawId, drawResultId);
  }

  private void expire(String drawId, String drawResultId) {
    notificationApi.expireByDedupeKeys(
        new ExpireNotificationsByDedupeKeysRequest(
            List.of(dedupeKey(drawId, drawResultId)), clock.instant()));
  }

  private static Map<String, NotificationTranslationInput> translations(
      DrawSettlementAttentionEvent event) {
    var fr =
        "%d ticket(s) restent à traiter pour le tirage du %s; échecs du dernier essai: %d."
            .formatted(event.pendingTickets(), event.drawDate(), event.failedTickets());
    return Map.of(
        "fr", new NotificationTranslationInput("Règlement de tirage à vérifier", fr),
        "en",
            new NotificationTranslationInput(
                "Draw settlement needs attention",
                "%d ticket(s) remain to process for the %s draw; failures in the last attempt: %d."
                    .formatted(event.pendingTickets(), event.drawDate(), event.failedTickets())),
        "ht",
            new NotificationTranslationInput(
                "Règleman tiraj la bezwen verifikasyon",
                "%d tikè rete pou trete pou tiraj %s la; echèk nan dènye esè a: %d."
                    .formatted(event.pendingTickets(), event.drawDate(), event.failedTickets())));
  }

  private static Map<String, Object> payload(DrawSettlementAttentionEvent event, String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "DRAW_SETTLEMENT_ATTENTION_REQUIRED");
    payload.put("tenantId", event.tenantId().value().toString());
    payload.put("drawId", event.drawId().value().toString());
    payload.put("drawResultId", event.drawResultId().value().toString());
    payload.put("drawChannelId", event.drawChannelId().value().toString());
    payload.put(
        "resultSlotId", event.resultSlotId() == null ? null : event.resultSlotId().value().toString());
    payload.put("drawDate", event.drawDate().toString());
    payload.put("resultedAt", event.resultedAt().toString());
    payload.put("pendingTickets", event.pendingTickets());
    payload.put("failedTickets", event.failedTickets());
    payload.put("correlationKey", dedupeKey);
    return payload;
  }
}
