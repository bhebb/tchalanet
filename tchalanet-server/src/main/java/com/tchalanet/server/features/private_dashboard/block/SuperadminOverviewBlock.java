package com.tchalanet.server.features.private_dashboard.block;

import java.util.List;

public record SuperadminOverviewBlock(
    int totalTenants,
    int activeTenants,
    List<String> notes
) {
    public static SuperadminOverviewBlock empty() {
        return new SuperadminOverviewBlock(0, 0, List.of());
    }
}
