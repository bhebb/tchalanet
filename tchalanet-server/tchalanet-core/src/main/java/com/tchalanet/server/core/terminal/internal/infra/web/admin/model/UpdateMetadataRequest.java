package com.tchalanet.server.core.terminal.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateMetadataRequest(@NotNull Map<String, Object> metadataPatch) {}
