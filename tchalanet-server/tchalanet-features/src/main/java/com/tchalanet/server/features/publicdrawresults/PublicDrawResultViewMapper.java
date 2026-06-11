package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultDetailView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultDetailResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultHistoryResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultLatestItem;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultLatestResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class PublicDrawResultViewMapper {

  private static final String DETAIL_PATH_PREFIX = "/public/results/";

  // ── Latest ─────────────────────────────────────────────────────────────────

  public PublicDrawResultLatestResponse toLatestResponse(
      List<PublicDrawResultSlotView> slots, Integer limit, Instant serverNow) {
    var stream = slots.stream();
    if (limit != null && limit > 0) {
      stream = stream.limit(limit);
    }
    return new PublicDrawResultLatestResponse(
        stream.map(this::toLatestItem).toList(),
        serverNow);
  }

  private PublicDrawResultLatestItem toLatestItem(PublicDrawResultSlotView slot) {
    var latest = slot.latest();
    var next = slot.next();
    var drawResultId = latest != null ? latest.drawResultId() : null;

    return new PublicDrawResultLatestItem(
        drawResultId,
        slot.slotKey(),
        slot.provider(),
        DrawChannelLabelKeyResolver.resolve(slot.slotKey()),
        slot.label(),
        latest != null ? latest.resultDate() : null,
        slot.drawTime(),
        slot.timezone(),
        latest != null ? latest.occurredAt() : null,
        latest != null ? latest.status() : null,
        latest != null ? extractNumbers(latest.haiti()) : List.of(),
        next != null ? next.expectedAt() : null,
        drawResultId != null ? DETAIL_PATH_PREFIX + drawResultId : null);
  }

  // ── History ────────────────────────────────────────────────────────────────

  public PublicDrawResultHistoryResponse toHistoryResponse(
      TchPage<PublicDrawResultHistoryRowView> page) {
    return new PublicDrawResultHistoryResponse(
        page.items().stream().map(this::toRow).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  private PublicDrawResultRow toRow(PublicDrawResultHistoryRowView row) {
    return new PublicDrawResultRow(
        row.drawResultId(),
        row.slotKey(),
        row.provider(),
        DrawChannelLabelKeyResolver.resolve(row.slotKey()),
        row.label(),
        row.resultDate(),
        row.drawTime(),
        row.timezone(),
        row.occurredAt(),
        row.status(),
        extractNumbers(row.haiti()),
        row.drawResultId() != null ? DETAIL_PATH_PREFIX + row.drawResultId() : null);
  }

  // ── Detail ─────────────────────────────────────────────────────────────────

  public PublicDrawResultDetailResponse toDetailResponse(PublicDrawResultDetailView view) {
    return new PublicDrawResultDetailResponse(
        view.drawResultId(),
        view.slotKey(),
        view.provider(),
        DrawChannelLabelKeyResolver.resolve(view.slotKey()),
        view.drawChannelLabel(),
        view.resultDate(),
        view.drawTime(),
        view.timezone(),
        view.occurredAt(),
        view.status(),
        extractNumbers(view.haiti()),
        view.sourceLabel(),
        view.publishedAt(),
        view.nextResultAt());
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Extrait les numéros tirés (lot1…lot4) d'un nœud JSON haïtien.
   * Les entrées null ou vides sont ignorées.
   */
  private List<String> extractNumbers(JsonNode haiti) {
    if (haiti == null || haiti.isNull() || haiti.isEmpty()) {
      return List.of();
    }
    var numbers = new ArrayList<String>(4);
    for (var field : List.of("lot1", "lot2", "lot3", "lot4")) {
      var node = haiti.get(field);
      if (node != null && !node.isNull() && !node.asText().isBlank()) {
        numbers.add(node.asText());
      }
    }
    return Collections.unmodifiableList(numbers);
  }
}
