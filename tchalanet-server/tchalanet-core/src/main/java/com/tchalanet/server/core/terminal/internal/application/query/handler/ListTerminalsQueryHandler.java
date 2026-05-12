package com.tchalanet.server.core.terminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.api.query.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTerminalsQueryHandler
    implements QueryHandler<ListTerminalsQuery, TchPage<TerminalSummaryView>> {

  private final TerminalReaderPort reader;

  @Override
  public TchPage<TerminalSummaryView> handle(ListTerminalsQuery q) {
    return reader.search(q.criteria(), q.pageRequest());
  }
}
