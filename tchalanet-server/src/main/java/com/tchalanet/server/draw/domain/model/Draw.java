package com.tchalanet.server.draw.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Draw(
    UUID id,
    UUID tenantId,
    UUID drawChannelId,
    String gameCode,
    Instant scheduledAt,
    Integer cutoffSec,
    String status,
    Map<String, Object> resultPayload,
    String drawSource,
    Boolean systemGenerated,
    Boolean locked,
    UUID createdBy,
    UUID updatedBy) {

  public Draw withScheduledAt(Instant scheduledAt) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withCutoffSec(Integer cutoffSec) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withStatus(String status) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withResultPayload(Map<String, Object> resultPayload) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withSystemGenerated(boolean systemGenerated) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        Boolean.valueOf(systemGenerated),
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withLocked(Boolean locked) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withLocked(boolean locked) {
    return withLocked(Boolean.valueOf(locked));
  }

  public Draw withUpdatedBy(UUID updatedBy) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withDrawSource(String drawSource) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withSystemGenerated(Boolean generated) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        generated,
        locked,
        createdBy,
        updatedBy);
  }

  public Draw withCreatedBy(UUID createdBy) {
    return new Draw(
        id,
        tenantId,
        drawChannelId,
        gameCode,
        scheduledAt,
        cutoffSec,
        status,
        resultPayload,
        drawSource,
        systemGenerated,
        locked,
        createdBy,
        updatedBy);
  }
}
