package com.tchalanet.server.core.terminal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTerminalByIdQueryHandler
    implements QueryHandler<GetTerminalByIdQuery, TerminalView> {

  private final TerminalReaderPort reader;
  private final TchContextResolver contextResolver;

  @Override
  public TerminalView handle(GetTerminalByIdQuery q) {
    var tenantId = contextResolver.currentOrThrow().tenantIdSafe();
    return TerminalView.from(reader.getRequired(tenantId, q.terminalId()));
  }
}
