package com.tchalanet.server.core.draw.infra.web.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public class UpdateDrawRequest {
  private OffsetDateTime scheduledAt;
  private Integer cutoffSec;
  private String status;
  private Boolean locked;
  private Map<String, Object> resultPayload;

  public OffsetDateTime getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(OffsetDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public Integer getCutoffSec() {
    return cutoffSec;
  }

  public void setCutoffSec(Integer cutoffSec) {
    this.cutoffSec = cutoffSec;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Boolean getLocked() {
    return locked;
  }

  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  public Map<String, Object> getResultPayload() {
    return resultPayload;
  }

  public void setResultPayload(Map<String, Object> resultPayload) {
    this.resultPayload = resultPayload;
  }
}
