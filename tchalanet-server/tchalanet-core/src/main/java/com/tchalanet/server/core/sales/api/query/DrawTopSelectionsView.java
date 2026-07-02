package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.types.id.DrawId;

import java.util.List;

public record DrawTopSelectionsView(
    DrawId drawId,
    List<SelectionItem> topSelections
) {
    public record SelectionItem(
        int rank,
        String displaySelection,
        String gameCode,
        String betType,
        Short betOption,
        int count,
        long totalStakeCents
    ) {}
}
