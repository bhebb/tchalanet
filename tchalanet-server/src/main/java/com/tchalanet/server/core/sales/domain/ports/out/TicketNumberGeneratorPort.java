package com.tchalanet.server.core.sales.domain.ports.out;

/** Outbound Port for generating a unique, internal ticket code (e.g., KSUID/ULID style). */
public interface TicketNumberGeneratorPort {
  String generate();
}
