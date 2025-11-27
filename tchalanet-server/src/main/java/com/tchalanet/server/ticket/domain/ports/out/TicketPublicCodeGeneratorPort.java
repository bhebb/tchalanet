package com.tchalanet.server.ticket.domain.ports.out;

/** Outbound Port for generating a short, human-readable public verification code. */
public interface TicketPublicCodeGeneratorPort {
  String generate();
}
