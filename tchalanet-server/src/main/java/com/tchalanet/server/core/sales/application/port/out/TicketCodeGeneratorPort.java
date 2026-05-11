package com.tchalanet.server.core.sales.application.port.out;

public interface TicketCodeGeneratorPort {
  String nextTicketCode();
  String nextPublicCode();
  String nextVerificationCode();
}
