package com.tchalanet.server.features.pagemodel.dynamic.providers.publicdrawresults;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.drawresult.api.query.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.api.query.SearchPublicDrawResultsQuery;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code public_draw_results}.
 *
 * Grouped reads (target ≤ 2 per dashboard-overview-runtime-v1 §12):
 *   1. {@link ListPublicDrawResultSlotsQuery} — systematic, latest + next per slot
 *   2. {@link SearchPublicDrawResultsQuery}  — conditional, bounded history when
 *      the widget props request it ({@code include_history=true}).
 *
 * Memoization is performed in {@link PublicDrawResultsProvider} using a key
 * derived from the props (slotKeys + provider + include_history + history_limit)
 * so two widgets with different needs in the same request don't collide.
 */
@Component
@RequiredArgsConstructor
public class PublicDrawResultsPayloadAssembler {

  static final int MIN_HISTORY_LIMIT = 1;
  static final int MAX_HISTORY_LIMIT = 100;

  private final QueryBus queryBus;

  public Payload assemble(Spec spec) {
    List<PublicDrawResultSlotView> slots =
        queryBus.ask(new ListPublicDrawResultSlotsQuery(spec.slotKeys(), spec.provider()));

    List<PublicDrawResultHistoryRowView> history = List.of();
    if (spec.includeHistory()) {
      int limit = Math.max(MIN_HISTORY_LIMIT,
          Math.min(MAX_HISTORY_LIMIT, spec.historyLimit()));
      var page = queryBus.ask(new SearchPublicDrawResultsQuery(
          spec.slotKeys(),
          spec.provider(),
          null,
          null,
          PageRequest.of(0, limit)));
      history = page != null && page.items() != null ? page.items() : List.of();
    }

    return new Payload(
        slots != null ? slots : List.of(),
        history);
  }

  /**
   * Specification of what to load, derived from a widget's props.
   */
  public record Spec(
      List<String> slotKeys,
      String provider,
      boolean includeHistory,
      int historyLimit) {

    public String memoKey() {
      return "public_draw_results:"
          + "slots=" + (slotKeys == null ? "" : String.join(",", slotKeys))
          + "|provider=" + (provider == null ? "" : provider)
          + "|history=" + includeHistory
          + "|limit=" + historyLimit;
    }
  }

  public record Payload(
      List<PublicDrawResultSlotView> slots,
      List<PublicDrawResultHistoryRowView> history) {}
}
