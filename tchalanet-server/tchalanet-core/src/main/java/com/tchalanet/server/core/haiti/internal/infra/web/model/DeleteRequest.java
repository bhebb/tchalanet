package com.tchalanet.server.core.haiti.internal.infra.web.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record DeleteRequest(@NotNull List<UUID> entryIds) {}
