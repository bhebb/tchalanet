package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record LimitContext(
    TenantId tenantId,
    OutletId outletId,
    UserId userId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    Instant now,
    List<LimitLineContext> lines
) {

    public LimitContext {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }

        if (now == null) {
            throw new IllegalArgumentException("now is required");
        }

        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public List<LimitScopeRef> scopes() {
        var scopes = new ArrayList<LimitScopeRef>();

        scopes.add(LimitScopeRef.tenant(tenantId));

        if (drawChannelId != null) {
            scopes.add(LimitScopeRef.drawChannel(drawChannelId));
        }

        if (outletId != null) {
            scopes.add(LimitScopeRef.outlet(outletId));
        }

        if (userId != null) {
            scopes.add(LimitScopeRef.agent(userId));
        }

        return List.copyOf(scopes);
    }

    public long totalStakeCents() {
        return lines.stream()
            .mapToLong(LimitLineContext::stakeCents)
            .sum();
    }

    public long totalPotentialPayoutCents() {
        return lines.stream()
            .mapToLong(LimitLineContext::potentialPayoutCents)
            .sum();
    }

    public int linesCount() {
        return lines.size();
    }
}
