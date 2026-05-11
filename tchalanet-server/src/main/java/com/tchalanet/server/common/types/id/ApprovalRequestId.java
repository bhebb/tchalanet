package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record ApprovalRequestId(UUID value) {
  public ApprovalRequestId {
    if (value == null) throw new IllegalArgumentException("ApprovalRequestId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static ApprovalRequestId of(UUID value) {
    return new ApprovalRequestId(value);
  }

  public static ApprovalRequestId nullableOf(UUID value) {
    return value == null ? null : new ApprovalRequestId(value);
  }

  public static ApprovalRequestId parse(String value) {
    return value == null ? null : new ApprovalRequestId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
