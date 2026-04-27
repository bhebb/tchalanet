package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.ExportDailySalesQuery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler to export daily sales to a CSV file. */
@UseCase
@RequiredArgsConstructor
@Component
public class ExportDailySalesQueryHandler implements QueryHandler<ExportDailySalesQuery, Path> {

  private final TicketReaderPort ticketReader;

  @Override
  public Path handle(ExportDailySalesQuery query) {
    Instant from = query.date().atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant to = query.date().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

    byte[] csvBytes = ticketReader.exportDailySalesCsv(from, to);

    try {
      Path tempFile = Files.createTempFile("daily_sales_", ".csv");
      Files.write(tempFile, csvBytes, StandardOpenOption.WRITE);
      return tempFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to write CSV file", e);
    }
  }
}
