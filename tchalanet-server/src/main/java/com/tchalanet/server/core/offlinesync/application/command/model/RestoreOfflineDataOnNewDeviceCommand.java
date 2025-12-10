package com.tchalanet.server.core.offlinesync.application.command.model;

import java.util.UUID;

public record RestoreOfflineDataOnNewDeviceCommand(UUID tenantId, UUID fromDeviceId, UUID toDeviceId) {}

