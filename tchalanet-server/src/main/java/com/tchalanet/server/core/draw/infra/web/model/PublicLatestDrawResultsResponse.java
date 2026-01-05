package com.tchalanet.server.core.draw.infra.web.model;

import java.time.Instant;
import java.util.List;

public record PublicLatestDrawResultsResponse(
    String channelCode,
    String channelName,
    String timezone,
    String drawTime,
    List<PublicDrawResultItemResponse> results,
    // next draw infos (optionnels)
    Instant nextScheduledAt,
    String nextDrawLabel,
    Boolean nextIsOpen,
    Boolean nextIsClosingSoon) {}
