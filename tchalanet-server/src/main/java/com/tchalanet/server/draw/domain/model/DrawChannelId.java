package com.tchalanet.server.draw.domain.model;

import java.util.UUID;

public final class DrawChannelId {
  private final UUID id;

  public DrawChannelId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID value() {
    return id;
  }

  @Override
  public String toString() {
    return id == null ? null : id.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DrawChannelId that = (DrawChannelId) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
