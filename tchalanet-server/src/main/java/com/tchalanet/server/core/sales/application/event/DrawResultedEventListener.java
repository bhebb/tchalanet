package com.tchalanet.server.core.sales.application.event;

import com.tchalanet.server.core.drawresult.domain.event.DrawResultedEvent;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultedEventListener {

    private final CommandBus commandBus;
    private final ResultSlotCatalog slotCatalog; // read-only ok
    private final DrawLookupPort drawLookupPort; // returns Optional<DrawId>

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResulted(DrawResultedEvent event) {
        log.info("DrawResultedEvent received: tenantId={} slotKey={} drawDate={} drawResultId={}",
            event.tenantId(), event.slotKey(), event.drawDate(), event.drawResultId());

        var slot = slotCatalog.findBySlotKey(event.slotKey())
            .orElseThrow(() -> new IllegalStateException("Unknown slotKey=" + event.slotKey()));

        var drawId = drawLookupPort.findDrawIdBySlotId(event.tenantId(), event.drawDate(), slot.id())
            .orElseThrow(() -> new IllegalStateException("No draw for tenant=" + event.tenantId()
                + " slotKey=" + event.slotKey() + " date=" + event.drawDate()));

        commandBus.send(new RecordDrawTicketsResultCommand(
            event.tenantId(), drawId, event.drawResultId(), event.occurredAt()));
    }
}

