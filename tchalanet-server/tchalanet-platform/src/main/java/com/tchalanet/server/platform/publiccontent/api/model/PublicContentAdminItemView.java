package com.tchalanet.server.platform.publiccontent.api.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PublicContentAdminItemView(
    UUID id,
    String title,
    String content,
    String imageUrl,
    String sourceUrl,
    PublicContentSourceType sourceType,
    PublicContentStatus status,
    Instant publishedAt,
    Instant expiresAt,
    Set<PublicContentSurface> targetSurfaces,
    String createdBy,
    Instant createdAt,
    String lastModifiedBy,
    Instant lastModifiedAt
) {}
