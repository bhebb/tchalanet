package com.tchalanet.server.core.draw.api;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummaryView;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.time.Duration;
import java.util.List;

/**
 * Public read-only port for Draw domain.
 * Accessible from other domains (e.g., core.drawresult).
 */
public interface DrawReaderPort {


    boolean existsSettledDrawForResult(DrawResultId drawResultId);

    List<DrawSummaryView> findByDrawResultId(DrawResultId drawResultId);

    List<DrawSummaryView> findResultedWithProvisionalOlderThan(Duration duration);
}
