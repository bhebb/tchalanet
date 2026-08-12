package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import jakarta.validation.constraints.NotNull;

public record UpdateDrawChannelActiveRequest(@NotNull Boolean active) {}
