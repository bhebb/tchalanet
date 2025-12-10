package com.tchalanet.server.core.offlinesync.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.query.model.GenerateOfflineReportQuery;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GenerateOfflineReportQueryHandler implements QueryHandler<GenerateOfflineReportQuery, Path> {

  @Override
  public Path handle(GenerateOfflineReportQuery query) {
    // TODO: generate offline report
    throw new UnsupportedOperationException("GenerateOfflineReportQueryHandler not implemented yet");
  }
}

