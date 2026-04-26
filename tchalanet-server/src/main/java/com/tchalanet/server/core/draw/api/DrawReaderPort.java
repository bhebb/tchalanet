package com.tchalanet.server.core.draw.api;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.time.Duration;
import java.util.List;

/**
 * Public read-only port for Draw domain.
 * Accessible from other domains (e.g., core.drawresult).
 */
public interface DrawReaderPort {

  /**
   * Check if any SETTLED draw exists for the given result.
   */
  boolean existsSettledDrawForResult(DrawResultId drawResultId);

  /**
   * Find all draws linked to a specific result.
   */
  List<DrawSummary> findByDrawResultId(DrawResultId drawResultId);

  /**
   * Find draws stuck in RESULTED state with a PROVISIONAL result for too long.
   */
  List<DrawSummary> findResultedWithProvisionalOlderThan(Duration duration);
}
