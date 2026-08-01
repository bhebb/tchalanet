package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.query.DrawsSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawsSummaryQuery;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawsSummaryQueryHandler
    implements QueryHandler<GetDrawsSummaryQuery, DrawsSummary> {

  private final DrawSummaryReaderPort reader;

  @Override
  public DrawsSummary handle(GetDrawsSummaryQuery query) {
    return reader.summarize(query.criteria(), query.today(), query.now());
  }
}
