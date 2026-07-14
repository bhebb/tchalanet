package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.api.query.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.PublicDrawResultSlotReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPublicDrawResultSlotsQueryHandler
    implements QueryHandler<ListPublicDrawResultSlotsQuery, List<PublicDrawResultSlotView>> {

  private final PublicDrawResultSlotReaderPort reader;

  @Override
  public List<PublicDrawResultSlotView> handle(ListPublicDrawResultSlotsQuery query) {
    return reader.listPublicSlots(
        PublicDrawResultQueryNormalizer.slotKeys(query.slotKeys()),
        PublicDrawResultQueryNormalizer.provider(query.provider()));
  }
}
