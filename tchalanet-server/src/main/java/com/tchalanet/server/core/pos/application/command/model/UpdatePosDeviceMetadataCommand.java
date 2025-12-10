package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;
import java.util.Map;

public record UpdatePosDeviceMetadataCommand(UUID tenantId, UUID deviceId, Map<String,Object> metadata) {}

