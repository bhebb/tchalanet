package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import java.util.List;

public record ListSyncPendingTerminalsQuery() implements Query<List<TerminalSummaryView>> {}
