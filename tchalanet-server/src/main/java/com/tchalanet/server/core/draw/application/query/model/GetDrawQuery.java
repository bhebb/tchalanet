package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import java.util.Optional;

public record GetDrawQuery(TenantId tenantId, DrawId drawId)
    implements Query<Optional<DrawResult>> {}
