package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateDrawChannelFlagsRequest(@NotNull JsonNode flags) {}
