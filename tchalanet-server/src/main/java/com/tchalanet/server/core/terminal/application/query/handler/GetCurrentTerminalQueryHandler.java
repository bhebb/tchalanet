package com.tchalanet.server.core.terminal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.GetCurrentTerminalQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentTerminalQueryHandler
    implements QueryHandler<GetCurrentTerminalQuery, TerminalView> {

  private final TerminalReaderPort reader;

  @Override
  public TerminalView handle(GetCurrentTerminalQuery q) {
    return reader
        .findActiveForUser(q.userId())
        .map(TerminalView::from)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No active terminal for user: " + q.userId().value()));
  }
}
