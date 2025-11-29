package com.tchalanet.server.core.draw.domain.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a draw has been successfully resulted (i.e., winning numbers are known).
 * This event triggers downstream processes like stats calculation and ticket settlement.
 */
@Getter
public class DrawResultedEvent extends ApplicationEvent {
  private final UUID drawId;
  private final UUID tenantId;
  private final String gameCode;
  private final String drawChannelCode;
  private final Instant scheduledAt;

  public DrawResultedEvent(
      Object source,
      UUID drawId,
      UUID tenantId,
      String gameCode,
      String drawChannelCode,
      Instant scheduledAt) {
    super(source);
    this.drawId = drawId;
    this.tenantId = tenantId;
    this.gameCode = gameCode;
    this.drawChannelCode = drawChannelCode;
    this.scheduledAt = scheduledAt;
  }
}
