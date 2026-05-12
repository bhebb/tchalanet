package com.tchalanet.server.core.terminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.api.query.ListSyncPendingTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListSyncPendingTerminalsQueryHandler
    implements QueryHandler<ListSyncPendingTerminalsQuery, List<TerminalSummaryView>> {

  private final TerminalReaderPort reader;

  @Override
  public List<TerminalSummaryView> handle(ListSyncPendingTerminalsQuery q) {
    return reader.listSyncPending();
  }
}
