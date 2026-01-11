package com.tchalanet.server.features.publicdraw.infra.web.model;

import java.time.Instant;

public record PublicNextDrawItem(
    String slotKey, String provider, String timezone, String drawTime, Instant nextScheduledAt) {}
