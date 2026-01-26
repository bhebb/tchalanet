package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateDrawChannelFlagsRequest(@NotNull JsonNode flags) {}
