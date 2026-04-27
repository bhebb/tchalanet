package com.tchalanet.server.core.payout.application.port.out;

import com.tchalanet.server.core.payout.domain.model.Payout;

public interface PayoutWriterPort {
  Payout save(Payout payout);
}
