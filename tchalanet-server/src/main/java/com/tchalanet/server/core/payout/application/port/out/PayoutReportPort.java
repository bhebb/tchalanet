package com.tchalanet.server.core.payout.port.out;

import com.tchalanet.server.core.payout.application.query.model.GeneratePayoutReportQuery;
import java.nio.file.Path;

public interface PayoutReportPort {
  Path generate(GeneratePayoutReportQuery query);
}

