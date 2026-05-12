package com.tchalanet.server.features.publicdrawresults.model;

import java.util.List;

public record PublicDrawResultListResponse(
    List<PublicDrawResultItemResponse> items,
    int page,
    int size,
    long totalItems,
    int totalPages
) {}
