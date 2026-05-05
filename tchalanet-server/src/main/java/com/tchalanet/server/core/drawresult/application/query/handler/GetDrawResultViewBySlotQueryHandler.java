package com.tchalanet.server.core.drawresult.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.application.exception.DrawResultNotFoundException;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.GetDrawResultViewBySlotQuery;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawResultViewBySlotQueryHandler
    implements QueryHandler<GetDrawResultViewBySlotQuery, DrawResultView> {

    private final DrawResultReaderPort reader;

    @Override
    public DrawResultView handle(GetDrawResultViewBySlotQuery query) {
        return reader.findViewBySlotKeyAndOccurredAt(query.slotKey(), query.occurredAt())
            .orElseThrow(() -> new DrawResultNotFoundException(
                "DrawResult not found for slot=" + query.slotKey() + " at=" + query.occurredAt()
            ));
    }
}
