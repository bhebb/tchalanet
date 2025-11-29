package com.tchalanet.server.draw.infra.web.dto;

import com.tchalanet.server.draw.domain.model.DrawSource;
import java.time.Instant;
import java.util.List;

public record DrawResultDto(
    DrawSource source,
    List<String> numbersMain,
    List<String> numbersExtra,
    Instant occurredAt,
    String rawPayload,
    boolean overridden,
    String overrideReason) {}
