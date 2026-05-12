package com.tchalanet.server.core.haiti.internal.infra.web.model;

import jakarta.validation.constraints.NotNull;

public record ImportRequest(
    String lang,
    @NotNull String payloadRef,
    @NotNull
        com.tchalanet.server.core.haiti.api.command.ImportTchalaEntriesCommand
                .ImportMode
            mode) {}
