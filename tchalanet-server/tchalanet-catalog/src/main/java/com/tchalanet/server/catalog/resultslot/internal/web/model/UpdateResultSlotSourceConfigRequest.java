package com.tchalanet.server.catalog.resultslot.internal.web.model;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record UpdateResultSlotSourceConfigRequest(@NotNull JsonNode sourceCfg) {}
