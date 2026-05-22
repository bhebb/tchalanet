package com.tchalanet.server.features.cashier.home.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;

public record CashierHomeOperationalContext(
    boolean ready,
    boolean trusted,
    String source,
    OutletId outletId,
    String outletName,
    TerminalId terminalId,
    String terminalLabel,
    SalesSessionId salesSessionId,
    List<String> missing) {}
