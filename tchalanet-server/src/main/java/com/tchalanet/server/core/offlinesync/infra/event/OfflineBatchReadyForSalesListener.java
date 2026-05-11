package com.tchalanet.server.core.offlinesync.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.domain.event.OfflineBatchReadyForSalesEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OfflineBatchReadyForSalesListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReady(OfflineBatchReadyForSalesEvent event) {
    commandBus.execute(new ProcessOfflineBatchWithSalesCommand(event.batchId()));
  }
}

