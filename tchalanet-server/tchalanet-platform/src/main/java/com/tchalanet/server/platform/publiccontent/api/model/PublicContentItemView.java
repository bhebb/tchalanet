package com.tchalanet.server.platform.publiccontent.api.model;

import java.time.Instant;
import java.util.UUID;

public record PublicContentItemView(
    UUID id,
    String title,
    String content,
    String imageUrl,
    String sourceUrl,
    PublicContentSourceType sourceType,
    Instant publishedAt
) {}
