package com.tchalanet.server.platform.ops.api;

public record OpsServiceResourceItem(
    String serviceKey,
    String displayName,
    String status,
    Integer memoryUsedMb,
    Integer memoryLimitMb,
    Integer memoryPercent,
    Double cpuPercent,
    Integer restartCount,
    Boolean oomKilled,
    String lastRestartAt,
    String severity,
    String message,
    String detailsPath,
    Integer sizeMb,
    Integer indexSizeMb,
    Integer tableCount) {}
