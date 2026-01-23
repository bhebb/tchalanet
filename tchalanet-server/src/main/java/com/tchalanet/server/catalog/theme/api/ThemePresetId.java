package com.tchalanet.server.catalog.theme.api;

import java.util.Objects;
import java.util.UUID;

public final class ThemePresetId {

    private final UUID value;

    private ThemePresetId(UUID value) {
        this.value = value;
    }

    public static ThemePresetId of(UUID id) {
        return new ThemePresetId(id);
    }

    public static ThemePresetId parse(String s) {
        return new ThemePresetId(UUID.fromString(s));
    }

    public UUID value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThemePresetId that = (ThemePresetId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
