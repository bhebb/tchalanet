package com.tchalanet.server.core.draw.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketSettlementQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettleWriter implements ItemWriter<DrawId> {

  private final DrawReaderPort drawReaderPort;
  private final DrawLifecyclePort drawWriterPort;
  private final TicketSettlementQueryPort ticketQuery;

  @Override
  public void write(Chunk<? extends DrawId> chunks) throws Exception {
    chunks
        .getItems()
        .forEach(
            drawId -> {
              var drawOpt = drawReaderPort.findById(drawId);
              if (drawOpt.isEmpty()) return;

              var draw = drawOpt.get();
              try {
                // only settle if status is RESULTED
                if (draw.status()
                    != com.tchalanet.server.core.draw.domain.model.DrawStatus.RESULTED) return;

                // 2) pas de result => never
                if (draw.result() == null) {
                  log.warn(
                      "settle.skip: draw={} tenant={} reason=no_result", drawId, draw.tenantId());
                  return;
                }

                // 3) business block: tickets not finalized
                if (ticketQuery.existsPendingByDrawId(draw.tenantId(), draw.id())) {
                  long pending = ticketQuery.countPendingByDrawId(draw.tenantId(), draw.id());
                  log.info(
                      "settle.skip: draw={} tenant={} pendingTickets={}",
                      drawId,
                      draw.tenantId(),
                      pending);
                  return;
                }

                // call domain method settle() and persist via writer port
                draw.settle();
                drawWriterPort.save(draw);
              } catch (Exception e) {
                log.error(
                    "SettleWriter: failed to settle draw={} cause={}", drawId, e.getMessage());
              }
            });
  }
}
