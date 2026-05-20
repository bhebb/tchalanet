package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.value.PublicCode;
import com.tchalanet.server.core.sales.api.model.value.TicketCode;
import com.tchalanet.server.core.sales.api.model.value.VerificationCode;

public interface TicketCodeGeneratorPort {
  TicketCode nextTicketCode();
  PublicCode nextPublicCode();
  VerificationCode nextVerificationCode();
}
