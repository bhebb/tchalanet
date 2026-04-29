package com.tchalanet.server.features.publicdraw.model;

import java.time.Instant;

public record PublicNextDrawItem(
    String slotKey, String provider, String timezone, String drawTime, Instant nextScheduledAt) {}
