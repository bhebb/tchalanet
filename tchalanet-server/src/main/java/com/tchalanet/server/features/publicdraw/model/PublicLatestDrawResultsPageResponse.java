package com.tchalanet.server.features.publicdraw.model;

import java.util.List;

public record PublicLatestDrawResultsPageResponse(
    List<PublicLatestDrawResultsResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages
) {}
