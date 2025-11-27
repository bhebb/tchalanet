package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.AdminUpdateDrawUseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.web.dto.UpdateDrawRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUpdateDrawUseCaseImpl implements AdminUpdateDrawUseCase {

  private final DrawRepository drawRepository;

  // todo fixme
  public Draw updateDraw(UUID tenantId, UUID drawId, Map<String, Object> updates, UUID adminId) {
    log.warn(
        "AdminUpdateDrawUseCaseImpl is a placeholder and does not implement actual update logic.");
    return drawRepository
        .findById(drawId)
        .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + drawId));
  }

  @Override
  public Draw updateDraw(UUID tenantId, UUID drawId, UpdateDrawRequest req, UUID adminId) {
    return null;
  }
}
