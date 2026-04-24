package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

/** Port to snapshot exposure facts for runtime evaluation. */
public interface ExposureFactsReaderPort {
  LimitFactsSnapshot snapshot(LimitContext ctx); // uses ctx.drawId + ctx.scope + betTypes in ctx.lines
}
