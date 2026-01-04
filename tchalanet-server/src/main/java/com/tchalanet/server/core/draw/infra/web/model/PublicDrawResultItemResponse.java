package com.tchalanet.server.core.draw.infra.web.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PublicDrawResultItemResponse(
    String channelCode,
    String channelName,
    LocalDate drawDate,
    Instant occurredAt,
    List<Integer> numbersMain,
    List<Integer> numbersExtra,
    String quality,
    String source) {}
