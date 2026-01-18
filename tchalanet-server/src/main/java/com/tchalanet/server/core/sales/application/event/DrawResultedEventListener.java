package com.tchalanet.server.core.sales.application.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.catalog.drawresult.domain.event.DrawResultedEvent;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultedEventListener {

  private final CommandBus commandBus;
  private final ResultSlotCatalog slotCatalog;
  private final DrawLookupPort drawLookupPort;

  @EventListener
  public void onDrawResulted(DrawResultedEvent event) {
    log.info(
        "DrawResultedEvent received: tenantId={} slotKey={} drawDate={} drawResultId={}",
        event.tenantId(),
        event.slotKey(),
        event.drawDate(),
        event.drawResultId());

    try {
      // resolve slot -> resultSlotId
      var slotOpt = slotCatalog.findBySlotKey(event.slotKey());
      if (slotOpt.isEmpty()) {
        log.warn("No result slot found for slotKey={}; skipping ticket settlement", event.slotKey());
        return;
      }
      var slot = slotOpt.get();

      // find drawId by slot id + date
      var drawIdOpt = drawLookupPort.findDrawIdBySlotId(event.tenantId(), event.drawDate(), slot.id().uuid());
      if (drawIdOpt.isEmpty()) {
        log.warn(
            "No draw found for tenant={} slotKey={} date={} ; skipping ticket settlement",
            event.tenantId(),
            event.slotKey(),
            event.drawDate());
        return;
      }

      DrawId drawId = DrawId.of(drawIdOpt.get());

      commandBus.send(
          new RecordDrawTicketsResultCommand(
              event.tenantId(), drawId, event.drawResultId(), event.occurredAt()));

    } catch (Exception ex) {
      log.error("Failed handling DrawResultedEvent for drawResultId={}: {}", event.drawResultId(), ex.toString());
    }
  }
}
