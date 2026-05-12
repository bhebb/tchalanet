package com.tchalanet.server.core.terminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.ListOfflineTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSummaryView;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListOfflineTerminalsQueryHandler
    implements QueryHandler<ListOfflineTerminalsQuery, List<TerminalSummaryView>> {

  private final TerminalReaderPort reader;

  @Override
  public List<TerminalSummaryView> handle(ListOfflineTerminalsQuery q) {
    return reader.listOffline();
  }
}
