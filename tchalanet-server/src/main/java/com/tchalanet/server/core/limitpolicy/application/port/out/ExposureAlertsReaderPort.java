package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.common.types.enums.BetType;

import java.math.BigDecimal;
import java.util.List;

public interface ExposureAlertsReaderPort {
  List<Row> topByStake(DrawId drawId, LimitScopeRef scope, int limit);
  List<Row> topByPayout(DrawId drawId, LimitScopeRef scope, int limit);

  record Row(BetType betType, String selectionKey,
             BigDecimal stakeTotal, BigDecimal potentialPayoutTotal, long salesCount) {}
}
