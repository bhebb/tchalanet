package com.tchalanet.server.core.haiti.infra.web.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApproveRequest(
    @NotNull UUID entryId,
    @NotNull
        com.tchalanet.server.core.haiti.application.command.model.ApproveTchalaEntryCommand
                .ApprovalMode
            mode,
    UUID targetCanonicalId,
    String mergePolicy) {}
