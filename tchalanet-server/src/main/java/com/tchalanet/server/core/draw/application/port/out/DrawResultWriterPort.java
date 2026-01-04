package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawResultOverrideMetadata;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawResultUpsertResult;
import java.time.LocalDate;

public interface DrawResultWriterPort {

  DrawResult save(TenantId tenantId, DrawResult result);

  DrawResult save(TenantId tenantId, DrawId drawId, DrawResult result);

  DrawResultUpsertResult upsertFromExternal(
      String channelCode,
      LocalDate drawDate,
      ExternalDrawResultPort.ExternalDrawResult ext,
      boolean force);

  DrawResult overrideResult(DrawResult result, DrawResultOverrideMetadata metadata);

  DrawResult invalidateResult(TenantId tenantId, DrawId drawId, String reason);
}
