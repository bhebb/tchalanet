package com.tchalanet.server.core.draw.infra.web.model;

import java.util.List;

public record PublicDrawResultPageResponse(
    List<PublicDrawResultItemResponse> items, int page, int size, long total) {}
