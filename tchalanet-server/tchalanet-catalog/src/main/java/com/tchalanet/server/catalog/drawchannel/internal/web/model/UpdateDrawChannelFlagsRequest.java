package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record UpdateDrawChannelFlagsRequest(@NotNull JsonNode flags) {}
