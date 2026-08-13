package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record UpdateTenantDrawChannelRequest(
    String name,
    String label,
    ZoneId timezone,
    LocalTime drawTime,
    LocalTime salesOpenTime,
    Integer cutoffSec,
    List<DayOfWeek> daysOfWeek,
    Boolean active,
    Integer sortOrder,
    String period,
    String notes) {}
