package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

public record ListTerminalsQuery(TerminalSearchCriteria criteria, TchPageRequest pageRequest)
    implements Query<TchPage<TerminalSummaryView>> {}
