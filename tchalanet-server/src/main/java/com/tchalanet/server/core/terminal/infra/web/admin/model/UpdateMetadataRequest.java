package com.tchalanet.server.core.terminal.infra.web.admin.model;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateMetadataRequest(@NotNull Map<String, Object> metadataPatch) {}
