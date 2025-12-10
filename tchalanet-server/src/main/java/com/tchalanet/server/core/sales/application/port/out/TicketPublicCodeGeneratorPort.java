package com.tchalanet.server.core.sales.application.port.out;

/** Outbound Port for generating a short, human-readable public verification code. */
public interface TicketPublicCodeGeneratorPort {
  String generate();
}
