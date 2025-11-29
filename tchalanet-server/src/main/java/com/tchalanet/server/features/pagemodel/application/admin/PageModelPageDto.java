package com.tchalanet.server.features.pagemodel.application.admin;

import java.util.List;

public record PageModelPageDto(
    List<PageModelSummaryDto> content, int totalPages, long totalElements, int size, int page) {}
