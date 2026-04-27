package com.tchalanet.server.features.news.admin.dto;

import com.tchalanet.server.features.news.shared.NewsStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUpsertNewsRequest(
    UUID id, // null = création
    String title,
    String description,
    String contentHtml,
    List<String> categories,
    Instant publishedAt,
    NewsStatus status // DRAFT / PUBLISHED / ARCHIVED
    ) {}
