package com.tchalanet.server.core.haiti.internal.infra.web.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApproveRequest(
    @NotNull UUID entryId,
    @NotNull
        com.tchalanet.server.core.haiti.api.command.ApproveTchalaEntryCommand
                .ApprovalMode
            mode,
    UUID targetCanonicalId,
    String mergePolicy) {}
