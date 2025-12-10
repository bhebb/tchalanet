package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.query.model.ExportDailySalesQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler stub to export daily sales to a file (CSV, XLSX, etc.). */
@UseCase
@RequiredArgsConstructor
@Component
public class ExportDailySalesQueryHandler implements QueryHandler<ExportDailySalesQuery, Path> {

  @Override
  public Path handle(ExportDailySalesQuery query) {
    // TODO: implement export and return path to generated file
    throw new UnsupportedOperationException("ExportDailySalesQueryHandler not implemented yet");
  }
}

