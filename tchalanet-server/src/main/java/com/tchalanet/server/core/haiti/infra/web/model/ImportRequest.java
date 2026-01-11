package com.tchalanet.server.core.haiti.infra.web.model;

import jakarta.validation.constraints.NotNull;

public record ImportRequest(
    String lang,
    @NotNull String payloadRef,
    @NotNull
        com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand
                .ImportMode
            mode) {}
