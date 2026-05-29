package com.tchalanet.server.core.payout.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import java.util.List;

public record ListPayoutClaimsForDrawQuery(
    DrawId drawId
) implements Query<List<PayoutClaimForDrawRow>> {}
