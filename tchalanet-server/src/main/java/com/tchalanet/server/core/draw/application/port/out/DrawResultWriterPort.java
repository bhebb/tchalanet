package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawResultOverrideMetadata;
import com.tchalanet.server.core.draw.domain.model.DrawResult;

public interface DrawResultWriterPort {
  DrawResult save(DrawResult result);

  DrawResult save(TenantId tenantId, DrawId drawId, DrawResult result);

  // pour override / invalidate si tu veux être explicite
  DrawResult overrideResult(DrawResult result, DrawResultOverrideMetadata metadata);

  DrawResult invalidateResult(TenantId tenantId, DrawId drawId, String reason);
}
