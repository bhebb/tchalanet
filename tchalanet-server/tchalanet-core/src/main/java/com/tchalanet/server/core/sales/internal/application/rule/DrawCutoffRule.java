package com.tchalanet.server.core.sales.internal.application.rule;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawCutoffRule {

  private final QueryBus queryBus;
  private final TchTimeProvider timeProvider;

  /**
   * Returns the resolved draw (reuse in handler) and ensures sale is still allowed.
   *
   * <p>The rule is: {@code now < draw.cutoffAt}. A sale arriving exactly at cutoff ({@code now ==
   * cutoffAt}) is rejected.
   */
  public DrawSummary requireBeforeCutoff(DrawId drawId) {
    var draw = queryBus.ask(new GetDrawByIdQuery(drawId));
    var now = timeProvider.now();
    var cutoff = draw.cutoffAt();

    if (draw.status() != DrawStatus.OPEN) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_NOT_OPEN);
    }

    if (!draw.drawChannelActive()) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_CHANNEL_INACTIVE);
    }

    if (!draw.resultActive()) {
      throw ProblemRest.of(SalesErrorCodes.RESULT_SLOT_INACTIVE);
    }

    if (!now.isBefore(cutoff)) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_CUTOFF_ELAPSED);
    }
    return draw;
  }

  public DrawSummary requireBeforeCutoff(DrawId drawId, DrawChannelId drawChannelId) {
    var draw = requireBeforeCutoff(drawId);
    if (!Objects.equals(draw.drawChannelId(), drawChannelId)) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_CHANNEL_MISMATCH);
    }
    return draw;
  }
}
