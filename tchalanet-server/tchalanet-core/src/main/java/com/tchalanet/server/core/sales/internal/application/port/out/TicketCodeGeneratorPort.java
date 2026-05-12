package com.tchalanet.server.core.sales.internal.application.port.out;

public interface TicketCodeGeneratorPort {
  String nextTicketCode();
  String nextPublicCode();
  String nextVerificationCode();
}
