package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AddressId(UUID value) {
  public AddressId {
    if (value == null) throw new IllegalArgumentException("AddressId.value is null");
  }

  public static AddressId of(UUID value) {
    return new AddressId(value);
  }

  public static AddressId nullableOf(UUID id) {
    return id == null ? null : new AddressId(id);
  }

  public static AddressId of(String id) {
    if (id == null) throw new IllegalArgumentException("address id string is required");
    return new AddressId(UUID.fromString(id));
  }

  public static AddressId random() {
    return new AddressId(UUID.randomUUID());
  }

  public UUID uuid() {
    return value;
  }

  public UUID value() {
    return value;
  }
}
