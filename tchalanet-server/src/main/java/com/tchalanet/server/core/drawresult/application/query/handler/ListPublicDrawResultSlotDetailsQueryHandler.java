package com.tchalanet.server.core.drawresult.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotDetailsQuery;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotDetailsView;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPublicDrawResultSlotDetailsQueryHandler
    implements QueryHandler<
        ListPublicDrawResultSlotDetailsQuery, List<PublicDrawResultSlotDetailsView>> {

  private static final int DEFAULT_HISTORY_LIMIT = 5;
  private static final int MAX_HISTORY_LIMIT = 10;

  private final PublicDrawResultSlotReaderPort reader;

  @Override
  public List<PublicDrawResultSlotDetailsView> handle(ListPublicDrawResultSlotDetailsQuery query) {
    return reader.listPublicSlotDetails(
        PublicDrawResultQueryNormalizer.slotKeys(query.slotKeys()),
        PublicDrawResultQueryNormalizer.provider(query.provider()),
        normalizeHistoryLimit(query.historyLimit()));
  }

  private static int normalizeHistoryLimit(int historyLimit) {
    if (historyLimit <= 0) {
      return DEFAULT_HISTORY_LIMIT;
    }

    return Math.min(historyLimit, MAX_HISTORY_LIMIT);
  }
}
