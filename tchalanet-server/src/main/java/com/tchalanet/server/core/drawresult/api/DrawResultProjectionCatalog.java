package com.tchalanet.server.core.drawresult.api;

import com.tchalanet.server.common.types.id.DrawResultId;
import java.util.Optional;

public interface DrawResultProjectionCatalog {
  Optional<DrawResultProjection> findById(DrawResultId drawResultId);
}
