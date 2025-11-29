package com.tchalanet.server.core.draw.application.command.model;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record CreateDrawChannelCommand(
    UUID tenantId,
    String code,
    String name,
    String gameCode,
    ZoneId timezone,
    LocalTime drawTime,
    Integer cutoffSec,
    List<String> daysOfWeek,
    boolean active) {}
