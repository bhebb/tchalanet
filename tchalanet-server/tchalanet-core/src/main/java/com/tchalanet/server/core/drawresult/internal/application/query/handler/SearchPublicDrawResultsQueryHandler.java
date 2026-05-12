package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.SearchPublicDrawResultsQuery;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultHistoryRowView;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchPublicDrawResultsQueryHandler
    implements QueryHandler<SearchPublicDrawResultsQuery, TchPage<PublicDrawResultHistoryRowView>> {

  private static final int MAX_DATE_RANGE_DAYS = 90;

  private final PublicDrawResultSlotReaderPort reader;
  private final Clock clock;

  @Override
  public TchPage<PublicDrawResultHistoryRowView> handle(SearchPublicDrawResultsQuery query) {
    var from = normalizeFrom(query.from());
    var to = normalizeTo(query.to(), from);

    return reader.searchPublicHistory(
        PublicDrawResultQueryNormalizer.slotKeys(query.slotKeys()),
        PublicDrawResultQueryNormalizer.provider(query.provider()),
        from,
        to,
        query.pageable());
  }

  private LocalDate normalizeFrom(LocalDate from) {
    return from == null ? LocalDate.now(clock).minusDays(7) : from;
  }

  private LocalDate normalizeTo(LocalDate to, LocalDate from) {
    var effectiveTo = to == null ? LocalDate.now(clock) : to;
    var maxTo = from.plusDays(MAX_DATE_RANGE_DAYS);
    return effectiveTo.isAfter(maxTo) ? maxTo : effectiveTo;
  }
}
