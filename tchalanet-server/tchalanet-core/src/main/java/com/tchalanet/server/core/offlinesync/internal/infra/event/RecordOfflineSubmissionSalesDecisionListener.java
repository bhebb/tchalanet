package com.tchalanet.server.core.offlinesync.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.sales.api.event.OfflineSubmissionAcceptedAsTicketEvent;
import com.tchalanet.server.core.sales.api.event.OfflineSubmissionRejectedBySalesEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordOfflineSubmissionSalesDecisionListener {

    private final CommandBus commandBus;

    @EventListener
    public void onAccepted(OfflineSubmissionAcceptedAsTicketEvent event) {
        log.debug("Recording SALES_ACCEPTED for submission={}", event.submissionId());
    }

    @EventListener
    public void onRejected(OfflineSubmissionRejectedBySalesEvent event) {
        log.debug("Recording SALES_REJECTED for submission={} value={}",
            event.submissionId(), event.rejectionCode());

    }
}
