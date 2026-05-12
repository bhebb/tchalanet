package com.tchalanet.server.core.haiti.internal.infra.web.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RejectRequest(@NotNull UUID entryId, String reason) {}
