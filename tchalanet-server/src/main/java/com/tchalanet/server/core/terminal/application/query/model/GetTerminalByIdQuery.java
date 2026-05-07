package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TerminalId;

public record GetTerminalByIdQuery(TerminalId terminalId) implements Query<TerminalView> {}
