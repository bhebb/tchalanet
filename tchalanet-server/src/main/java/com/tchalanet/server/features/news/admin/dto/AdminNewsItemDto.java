package com.tchalanet.server.features.news.admin.dto;

import com.tchalanet.server.features.news.shared.NewsStatus;
import java.time.Instant;

public record AdminNewsItemDto(
    String id,
    String sourceId, // "internal" vs "lotterydaily"
    String title,
    String description,
    NewsStatus status, // DRAFT / PUBLISHED / ARCHIVED
    boolean hidden, // dans la liste "hidden" du cache
    Instant publishedAt) {}
