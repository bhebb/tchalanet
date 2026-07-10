package com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.catalog.game.api.model.BetType;

import java.math.BigDecimal;
import java.util.List;

public interface ExposureAlertsReaderPort {
  List<Row> topByStake(DrawId drawId, LimitScopeRef scope, int limit);
  List<Row> topBySettlementPayoutExposure(DrawId drawId, LimitScopeRef scope, int limit);

  record Row(BetType betType, String selectionKey,
             BigDecimal stakeTotal, BigDecimal settlementPayoutExposureTotal, long salesCount) {}
}
