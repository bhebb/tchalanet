package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.internal.application.exception.DrawResultNotFoundException;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewBySlotQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
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
