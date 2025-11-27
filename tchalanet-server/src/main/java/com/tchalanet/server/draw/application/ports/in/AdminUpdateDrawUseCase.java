package com.tchalanet.server.draw.application.ports.in;

import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.web.dto.UpdateDrawRequest;
import java.util.UUID;

public interface AdminUpdateDrawUseCase {
  Draw updateDraw(UUID tenantId, UUID drawId, UpdateDrawRequest req, UUID adminId);
}
