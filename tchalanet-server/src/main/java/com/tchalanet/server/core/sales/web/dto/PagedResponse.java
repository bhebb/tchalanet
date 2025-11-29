package com.tchalanet.server.core.sales.web.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, long totalItems, int totalPages, int currentPage) {}
