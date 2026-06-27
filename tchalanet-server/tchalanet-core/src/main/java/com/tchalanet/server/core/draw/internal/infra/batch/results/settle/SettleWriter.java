package com.tchalanet.server.core.draw.internal.infra.batch.results.settle;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.sales.internal.application.query.model.CountPendingTicketsByDrawIdQuery;
import com.tchalanet.server.core.sales.internal.application.query.model.ExistsPendingTicketsByDrawIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettleWriter implements ItemWriter<DrawId> {

    private final DrawLookupPort drawReaderPort;
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Override
    public void write(Chunk<? extends DrawId> chunk) {
        for (DrawId drawId : chunk.getItems()) {
            var drawOpt = drawReaderPort.findById(drawId);
            if (drawOpt.isEmpty()) continue;

            var draw = drawOpt.get();
            try {
                if (draw.status() != DrawStatus.RESULTED) {
                    continue;
                }

                if (draw.drawResultId() == null) {
                    log.warn("settle.skip draw={} tenant={} reason=no_result", drawId, draw.tenantId());
                    continue;
                }

                boolean existsPending = queryBus.ask(new ExistsPendingTicketsByDrawIdQuery(draw.id()));
                if (existsPending) {
                    long pending = queryBus.ask(new CountPendingTicketsByDrawIdQuery(draw.id()));
                    log.info("settle.skip draw={} tenant={} pendingTickets={}", drawId, draw.tenantId(), pending);
                    continue;
                }

                commandBus.execute(new SettleDrawCommand(List.of(drawId), null, false));
            } catch (Exception e) {
                log.error("settle.fail draw={} cause={}", drawId, e.getMessage(), e);
            }
        }
    }
}
