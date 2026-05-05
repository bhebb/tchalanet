package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.domain.model.Draw;

import java.util.Optional;

public interface DrawLookupPort {

    Optional<Draw> findById(DrawId drawId);

    Draw getById(DrawId drawId);

    Draw getByIdForUpdate(DrawId drawId);

    boolean existsSettledDrawForResult(DrawResultId drawResultId);

}
