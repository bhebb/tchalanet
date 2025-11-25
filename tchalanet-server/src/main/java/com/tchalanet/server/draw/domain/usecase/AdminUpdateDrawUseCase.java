package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.model.Draw;
import java.util.UUID;

public interface AdminUpdateDrawUseCase {
  Draw updateDraw(
      UUID tenantId,
      UUID drawId,
      com.tchalanet.server.draw.web.dto.UpdateDrawRequest req,
      UUID adminId);
}
