package com.tchalanet.server.features.pos.home.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import java.util.List;

public record PosHomeOperationalContext(
    boolean ready,
    boolean trusted,
    String source,
    SellerTerminalId sellerTerminalId,
    String sellerTerminalLabel,
    List<String> missing) {}
