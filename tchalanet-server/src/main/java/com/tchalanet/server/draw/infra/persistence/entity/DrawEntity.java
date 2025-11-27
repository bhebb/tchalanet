package com.tchalanet.server.draw.infra.persistence.entity;

import com.tchalanet.server.draw.domain.model.DrawStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DrawEntity {
  private UUID id;
  private UUID tenantId;
  private UUID drawChannelId;
  private String gameCode;
  private DrawStatus status;
  private Instant scheduledAt;
  private long cutoffSec;

  // additional fields expected by mappers
  private String drawSource;
  private Map<String, Object> resultPayload;
  private Boolean systemGenerated;
  private Boolean locked;
  private UUID createdBy;
  private UUID updatedBy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getDrawChannelId() {
    return drawChannelId;
  }

  public void setDrawChannelId(UUID drawChannelId) {
    this.drawChannelId = drawChannelId;
  }

  public String getGameCode() {
    return gameCode;
  }

  public void setGameCode(String gameCode) {
    this.gameCode = gameCode;
  }

  public DrawStatus getStatus() {
    return status;
  }

  public void setStatus(DrawStatus status) {
    this.status = status;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(Instant scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public long getCutoffSec() {
    return cutoffSec;
  }

  public void setCutoffSec(long cutoffSec) {
    this.cutoffSec = cutoffSec;
  }

  public String getDrawSource() {
    return drawSource;
  }

  public void setDrawSource(String drawSource) {
    this.drawSource = drawSource;
  }

  public Map<String, Object> getResultPayload() {
    return resultPayload;
  }

  public void setResultPayload(Map<String, Object> resultPayload) {
    this.resultPayload = resultPayload;
  }

  public Boolean getSystemGenerated() {
    return systemGenerated;
  }

  public void setSystemGenerated(Boolean systemGenerated) {
    this.systemGenerated = systemGenerated;
  }

  public Boolean getLocked() {
    return locked;
  }

  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(UUID updatedBy) {
    this.updatedBy = updatedBy;
  }
}
