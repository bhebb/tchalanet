package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.Draw;

public record GetDrawQuery(DrawId drawId)
    implements Query<Draw> {}
