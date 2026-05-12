package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;

import java.time.Duration;
import java.util.List;

/**
 * Public read-only port for Draw domain.
 * Accessible from other domains (e.g., core.drawresult).
 */
public interface DrawReaderPort {


    boolean existsSettledDrawForResult(DrawResultId drawResultId);

    List<DrawSummary> findByDrawResultId(DrawResultId drawResultId);

    List<DrawSummary> findResultedWithProvisionalOlderThan(Duration duration);
}
