package com.tchalanet.server.core.uslottery.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Represents a draw result recently fetched from an external provider. This is a value object. */
public record LatestDraw(
    UsLotteryProvider provider,
    String externalKey,
    String channelCode,
    LocalDate drawDate,
    OffsetDateTime drawTimeUtc,
    List<String> numbersRaw,
    String origin) {}
