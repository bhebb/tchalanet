package com.tchalanet.server.core.sales.api.query;

import java.time.LocalDate;
import java.util.List;

public record CashierTopSelectionsView(
    LocalDate businessDate,
    List<DrawGroup> byDraw
) {
    public record DrawGroup(
        String channelCode,
        String channelLabel,
        List<SelectionItem> topSelections
    ) {}

    public record SelectionItem(
        int rank,
        String displaySelection,
        String gameCode,
        int count,
        long totalStakeCents
    ) {}
}
