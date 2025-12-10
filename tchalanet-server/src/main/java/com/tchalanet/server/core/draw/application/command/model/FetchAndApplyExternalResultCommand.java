package com.tchalanet.server.core.draw.application.command.model;

import java.time.Instant;
import java.util.UUID;

public record FetchAndApplyExternalResultCommand(UUID drawId, Instant executedAt) {}
