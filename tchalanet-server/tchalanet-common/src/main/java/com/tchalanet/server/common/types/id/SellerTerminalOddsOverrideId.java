package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SellerTerminalOddsOverrideId(UUID value) {

    public SellerTerminalOddsOverrideId {
        if (value == null) throw new IllegalArgumentException("SellerTerminalOddsOverrideId value is null");
    }

    public static SellerTerminalOddsOverrideId of(UUID value) {
        return new SellerTerminalOddsOverrideId(value);
    }

    public static SellerTerminalOddsOverrideId parse(String value) {
        return value == null ? null : new SellerTerminalOddsOverrideId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
