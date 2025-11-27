package com.tchalanet.server.ticket.application;

import com.tchalanet.server.ticket.domain.ports.in.ArchiveTicketsUseCase;
import com.tchalanet.server.ticket.domain.ports.out.TicketRepositoryPort;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveTicketsService implements ArchiveTicketsUseCase {

  private final TicketRepositoryPort ticketRepository;

  @Override
  @Transactional
  public int archiveTickets(UUID tenantId, Instant cutoffDate) {
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(cutoffDate, "Cutoff date cannot be null");

    log.info("Archiving tickets for tenant {} created before {}", tenantId, cutoffDate);

    int archivedCount = ticketRepository.archiveOldTickets(tenantId, cutoffDate);

    log.info("Successfully archived {} tickets for tenant {}", archivedCount, tenantId);
    return archivedCount;
  }
}
