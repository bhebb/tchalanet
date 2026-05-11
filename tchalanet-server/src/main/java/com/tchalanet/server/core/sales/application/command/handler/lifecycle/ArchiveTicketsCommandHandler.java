package com.tchalanet.server.core.sales.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.ArchiveTicketsCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveTicketsCommandHandler implements VoidCommandHandler<ArchiveTicketsCommand> {

  private final TicketWriterPort ticketRepository;

  @Override
  @TchTx
  public void handle(ArchiveTicketsCommand command) {
    Objects.requireNonNull(command, "Command cannot be null");
    Objects.requireNonNull(command.cutoffDate(), "Cutoff date cannot be null");

    log.info(
        "Archiving tickets for tenant {} created before {}",
        command.tenantId(),
        command.cutoffDate());

    int archivedCount =
        ticketRepository.archiveOldTickets(command.cutoffDate());

    log.info("Successfully archived {} tickets", archivedCount);
  }
}
