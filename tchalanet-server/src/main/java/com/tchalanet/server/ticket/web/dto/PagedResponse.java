package com.tchalanet.server.ticket.web.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, long totalItems, int totalPages, int currentPage) {}
