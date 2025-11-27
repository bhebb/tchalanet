package com.tchalanet.server.draw.application.ports.in.command;

import java.util.UUID;

public record RefreshDrawCacheCommand(UUID tenantId) {}
