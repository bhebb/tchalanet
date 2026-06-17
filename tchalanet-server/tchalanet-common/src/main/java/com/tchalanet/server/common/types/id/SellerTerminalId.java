package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SellerTerminalId(UUID value) {
    public SellerTerminalId {
        if (value == null) throw new IllegalArgumentException("SellerTerminalId value is null");
    }

    public UUID uuid() {
        return value;
    }

    public static SellerTerminalId of(UUID value) {
        return new SellerTerminalId(value);
    }

    public static SellerTerminalId nullableOf(UUID value) {
        return value == null ? null : new SellerTerminalId(value);
    }

    public static SellerTerminalId parse(String value) {
        return value == null ? null : new SellerTerminalId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
