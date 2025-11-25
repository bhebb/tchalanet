package com.tchalanet.server.ticket.domain.usecase;

public interface ExpireTicketsUseCase {
  /** Mark tickets expired according to configured TTL/expiration rules. */
  void expireOldTickets();
}
