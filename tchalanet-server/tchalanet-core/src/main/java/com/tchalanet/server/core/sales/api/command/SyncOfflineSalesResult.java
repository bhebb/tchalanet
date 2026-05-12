package com.tchalanet.server.core.sales.api.command;

import java.util.List;

public record SyncOfflineSalesResult(
    int acceptedCount,
    int rejectedCount,
    int reviewCount,
    int conflictCount,
    List<SyncOfflineTicketDecision> decisions
) {}

