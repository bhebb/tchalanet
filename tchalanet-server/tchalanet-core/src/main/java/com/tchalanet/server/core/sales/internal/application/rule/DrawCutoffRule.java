package com.tchalanet.server.core.sales.internal.application.rule;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
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
     * <p>The rule is: {@code now < draw.cutoffAt}.
     * A sale arriving exactly at cutoff ({@code now == cutoffAt}) is rejected.
     */
    public DrawSummary requireBeforeCutoff(DrawId drawId) {
        var draw = queryBus.ask(new GetDrawByIdQuery(drawId));
        var now = timeProvider.now();
        var cutoff = draw.cutoffAt();

        if (draw.status() != DrawStatus.OPEN) {
            throw ProblemRest.conflict("Draw is not open for sales");
        }

        if (!now.isBefore(cutoff)) {
            throw ProblemRest.conflict("Draw cutoff time has passed");
        }
        return draw;
    }
}
