package com.tchalanet.server.core.draw.internal.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.sales.application.port.out.TicketSettlementQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettleWriter implements ItemWriter<DrawId> {

    private final DrawLookupPort drawReaderPort;
    private final DrawLifecyclePort drawWriterPort;
    private final TicketSettlementQueryPort ticketQuery;
    private final Clock clock;
    private final TchContextResolver contextResolver;

    @Override
    public void write(Chunk<? extends DrawId> chunk) {
        var now = Instant.now(clock);

        var ctx = contextResolver.currentOrNull();
        var tenantZone = ctx != null && ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneId.of("UTC");

        ZonedDateTime settledAt = now.atZone(tenantZone);

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

                if (ticketQuery.existsPendingByDrawId(draw.id())) {
                    long pending = ticketQuery.countPendingByDrawId(draw.id());
                    log.info("settle.skip draw={} tenant={} pendingTickets={}", drawId, draw.tenantId(), pending);
                    continue;
                }

                // Source truth = Instant now; conversion explicit via tenantZone
                draw.settle(settledAt.toInstant());

                drawWriterPort.save(draw);
            } catch (Exception e) {
                log.error("settle.fail draw={} cause={}", drawId, e.getMessage(), e);
            }
        }
    }

}
