package com.tchalanet.server.core.payout.infra.report;

import com.tchalanet.server.core.payout.application.port.out.PayoutReportPort;
import com.tchalanet.server.core.payout.application.query.model.GeneratePayoutReportQuery;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FilesystemPayoutReportAdapter implements PayoutReportPort {

  @Override
  public Path generate(GeneratePayoutReportQuery query) {
    try {
      Path tmp = Files.createTempFile("payout-report-", ".csv");
      try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
        w.write(
            "payout_id,ticket_id,paying_outlet,selling_outlet,amount_cents,currency,status,created_at,approved_at,paid_at\n");
        // V1: we don't fill rows here; adapter can be extended to query PayoutReaderPort
      }
      return tmp;
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate payout report", e);
    }
  }
}
