package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateDrawChannelGameRequest(
    Boolean enabled,
    JsonNode flags) {}
