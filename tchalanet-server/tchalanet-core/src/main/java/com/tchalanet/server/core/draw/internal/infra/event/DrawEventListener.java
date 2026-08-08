package com.tchalanet.server.core.draw.internal.infra.event;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.core.draw.api.event.DrawCancelledEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.internal.infra.cache.DrawCacheEvictor;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawEventListener {

  private static final String KEY_DRAW_RESULT_APPLIED_CACHE_EVICT =
      "draw.cache.evict.result_applied";
  private static final String KEY_DRAW_RESULT_CORRECTED_CACHE_EVICT =
      "draw.cache.evict.result_corrected";
  private static final String KEY_DRAW_SETTLED_CACHE_EVICT = "draw.cache.evict.settled";
  private static final String KEY_DRAW_CANCELLED_CACHE_EVICT = "draw.cache.evict.cancelled";

  private final ProcessedEventPort processedEventPort;
  private final DrawCacheEvictor drawCacheEvictor;
  private final NotificationApi notificationApi;
  private final JsonUtils jsonUtils;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawResultApplied(DrawResultAppliedEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_DRAW_RESULT_APPLIED_CACHE_EVICT, event.eventId().value())) {
      return;
    }

    drawCacheEvictor.evictAll();
    createTenantResultAppliedNotification(event);

    log.debug(
        "draw.cache.evicted reason=DrawResultApplied tenantId={} drawId={} resultSlotId={}",
        event.tenantId(),
        event.drawId(),
        event.resultSlotId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_DRAW_RESULT_CORRECTED_CACHE_EVICT, event.eventId().value())) {
      return;
    }

    drawCacheEvictor.evictAll();

    log.debug(
        "draw.cache.evicted reason=DrawResultCorrected tenantId={} drawId={} resultSlotId={}",
        event.tenantId(),
        event.drawId(),
        event.resultSlotId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawSettled(DrawSettledEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_DRAW_SETTLED_CACHE_EVICT, event.eventId().value())) {
      return;
    }

    drawCacheEvictor.evictAll();

    log.debug(
        "draw.cache.evicted reason=DrawSettled tenantId={} drawId={} resultSlotId={}",
        event.tenantId(),
        event.drawId(),
        event.resultSlotId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onDrawCancelled(DrawCancelledEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(
        KEY_DRAW_CANCELLED_CACHE_EVICT, event.eventId().value())) {
      return;
    }

    drawCacheEvictor.evictAll();

    log.debug(
        "draw.cache.evicted reason=DrawCancelled tenantId={} drawId={}",
        event.tenantId(),
        event.drawId());
  }

  private void createTenantResultAppliedNotification(DrawResultAppliedEvent event) {
    var dedupeKey =
        "draw.result.applied:%s:%s:TENANT_ADMINS"
            .formatted(event.drawId().value(), event.drawResultId().value());
    var title = "Résultat appliqué";
    var message = "Le tirage du %s a reçu son résultat.".formatted(event.drawDate());

    notificationApi.createNotification(
        new CreateNotificationRequest(
            event.tenantId(),
            "DRAW_RESULT_APPLIED",
            event.drawId().value().toString(),
            dedupeKey,
            NotificationAudienceType.TENANT_ADMINS,
            Set.of(),
            NotificationSeverity.INFO,
            NotificationKind.INFO,
            NotificationCategory.DRAW,
            null,
            null,
            title,
            message,
            resultAppliedTranslations(event.drawDate().toString()),
            jsonUtils.toJsonNode(resultAppliedPayload(event, dedupeKey)),
            "DRAW_RESULT_VIEW",
            "/app/admin/draws/" + event.drawId().value(),
            null,
            Set.of(NotificationChannel.WEB)));
  }

  private static Map<String, NotificationTranslationInput> resultAppliedTranslations(
      String drawDate) {
    return Map.of(
        "fr",
            new NotificationTranslationInput(
                "Résultat appliqué", "Le tirage du %s a reçu son résultat.".formatted(drawDate)),
        "en",
            new NotificationTranslationInput(
                "Result applied", "The draw for %s received its result.".formatted(drawDate)),
        "ht",
            new NotificationTranslationInput(
                "Rezilta aplike", "Tiraj %s la resevwa rezilta li.".formatted(drawDate)));
  }

  private static Map<String, Object> resultAppliedPayload(
      DrawResultAppliedEvent event, String dedupeKey) {
    var payload = new LinkedHashMap<String, Object>();
    payload.put("type", "DRAW_RESULT_APPLIED");
    payload.put("tenantId", event.tenantId().value().toString());
    payload.put("drawId", event.drawId().value().toString());
    payload.put("drawResultId", event.drawResultId().value().toString());
    payload.put("resultSlotId", event.resultSlotId().value().toString());
    payload.put("drawChannelId", event.drawChannelId().value().toString());
    payload.put("drawDate", event.drawDate().toString());
    payload.put("audienceType", "TENANT_ADMINS");
    payload.put("correlationKey", dedupeKey);
    return payload;
  }
}
