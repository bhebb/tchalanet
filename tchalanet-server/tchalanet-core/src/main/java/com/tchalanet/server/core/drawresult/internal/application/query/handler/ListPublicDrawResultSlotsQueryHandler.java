package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotView;
import lombok.RequiredArgsConstructor;
import java.util.List;

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
