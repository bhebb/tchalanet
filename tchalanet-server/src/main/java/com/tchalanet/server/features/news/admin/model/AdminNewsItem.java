package com.tchalanet.server.features.news.admin.model;

import com.tchalanet.server.features.news.shared.NewsStatus;
import java.time.Instant;

public record AdminNewsItem(
    String id,
    String sourceId, // "internal" vs "lotterydaily"
    String title,
    String description,
    NewsStatus status, // DRAFT / PUBLISHED / ARCHIVED
    boolean hidden, // dans la liste "hidden" du cache
    Instant publishedAt) {}
