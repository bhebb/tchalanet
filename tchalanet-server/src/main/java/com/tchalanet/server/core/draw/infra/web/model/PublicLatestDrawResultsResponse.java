package com.tchalanet.server.core.draw.infra.web.model;

import java.util.List;

public record PublicLatestDrawResultsResponse(
    String channelCode,
    String channelName,
    String timezone,
    String drawTime,
    List<PublicDrawResultItemResponse> results) {}
