package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.query.model.GenerateOutletReportQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GenerateOutletReportHandler implements QueryHandler<GenerateOutletReportQuery, Path> {

  @Override
  public Path handle(GenerateOutletReportQuery query) {
    // TODO: generate PDF/Excel
    throw new UnsupportedOperationException("GenerateOutletReportHandler not implemented yet");
  }
}

