package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase;
import com.tchalanet.server.draw.domain.events.DrawResultedEvent;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyDrawResultService implements ApplyDrawResultUseCase {

  private final DrawRepository drawRepository;
  private final ApplicationEventPublisher eventPublisher; // To publish DrawResultedEvent

  @Override
  @Transactional
  public Draw applyResult(ApplyDrawResultCommand command) {
    Draw draw =
        drawRepository
            .findById(command.drawId())
            .orElseThrow(
                () -> new IllegalArgumentException("Draw not found with id: " + command.drawId()));

    if (!draw.tenantId().equals(command.tenantId())) {
      throw new SecurityException("Tenant mismatch for draw " + command.drawId());
    }

    // Apply result using the rich domain model
    Draw updatedDraw = draw.applyResult(command.payload(), command.source(), command.appliedBy());

    drawRepository.save(updatedDraw);

    // Publish DrawResultedEvent
    eventPublisher.publishEvent(
        new DrawResultedEvent(
            this,
            updatedDraw.id(),
            updatedDraw.tenantId(),
            updatedDraw.gameCode(),
            updatedDraw
                .drawChannelId()
                .toString(), // Assuming drawChannelId can be converted to String code
            updatedDraw.scheduledAt()));

    log.info(
        "Draw {} for tenant {} resulted successfully by {} from source {}",
        updatedDraw.id(),
        updatedDraw.tenantId(),
        command.appliedBy(),
        command.source());

    return updatedDraw;
  }
}
