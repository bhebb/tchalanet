package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;

public record GetDrawResultProjectionByDrawIdQuery(
    DrawId drawId
) implements Query<DrawResultProjection> {}
