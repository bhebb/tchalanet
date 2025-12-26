package com.tchalanet.server.common.types.id;

import java.util.Objects;
import java.util.UUID;

public record RoleId(UUID uuid) {
    public RoleId { Objects.requireNonNull(uuid, "role id"); }
    public static RoleId of(UUID id) { return new RoleId(Objects.requireNonNull(id)); }
    /**
     * Create a RoleId or return null if the given UUID is null. Useful for optional parentRoleId mappings.
     */
    public static RoleId nullableOf(UUID id) { return id == null ? null : new RoleId(id); }
    public UUID uuid() { return uuid; }
}
