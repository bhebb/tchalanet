package com.tchalanet.server.uslottery.domain.dto;

import java.time.Instant;

/** DTO interne représentant un tirage externe US Lottery. */
public record LatestDrawDto(
    String externalChannelCode, Instant scheduledAt, String resultPayloadJson) {}
