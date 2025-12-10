package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.core.offlinesync.domain.model.OfflineTicket;
import java.util.List;
import java.util.UUID;

/** Port de persistance / lecture pour les tickets offline (inbox). */
public interface OfflineTicketPort {

  void register(OfflineTicket ticket);

  List<OfflineTicket> findPendingForTerminal(UUID tenantId, UUID terminalId, int limit);

  void markAsSynced(UUID ticketId);
}
