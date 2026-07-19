package com.tchalanet.server.core.drawresult.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import com.tchalanet.server.core.drawresult.api.query.GetPublicDrawResultDetailByIdQuery;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultDetailView;
import com.tchalanet.server.core.drawresult.internal.application.port.out.PublicDrawResultSlotReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPublicDrawResultDetailByIdQueryHandler
    implements QueryHandler<GetPublicDrawResultDetailByIdQuery, PublicDrawResultDetailView> {

  private final PublicDrawResultSlotReaderPort reader;

  @Override
  public PublicDrawResultDetailView handle(GetPublicDrawResultDetailByIdQuery query) {
    return reader
        .findPublicResultDetailById(query.id())
        .orElseThrow(() -> ProblemRest.of(DrawResultErrorCodes.NOT_FOUND));
  }
}
