package com.tchalanet.server.core.draw.application.query.projection;

import java.time.LocalDate;
import java.util.UUID;

/** Key describing an existing draw by channel and local draw date. */
public record ExistingDrawKey(UUID drawChannelId, LocalDate drawDate) {}
