package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawApplyJdbcRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawApplyJdbcAdapter implements DrawApplyPort {

  private final DrawApplyJdbcRepository repo;

  @Override
  public ApplyOutcome attachResultAndMarkResulted(UUID drawId, UUID drawResultId, boolean force) {
    int updated = repo.attachResult(drawId, drawResultId, force);
    return updated > 0 ? ApplyOutcome.UPDATED : ApplyOutcome.ALREADY_LINKED_OR_NOT_ELIGIBLE;
  }
}
