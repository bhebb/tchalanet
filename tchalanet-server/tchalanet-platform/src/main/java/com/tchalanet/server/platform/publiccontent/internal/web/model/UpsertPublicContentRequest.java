package com.tchalanet.server.platform.publiccontent.internal.web.model;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSurface;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;

public record UpsertPublicContentRequest(
    /** Null for create, non-null for update. */
    String id,
    @NotBlank String title,
    String content,
    String contentHtml,
    String imageUrl,
    String sourceUrl,
    PublicContentStatus status,
    Set<PublicContentSurface> targetSurfaces,
    Instant publishedAt,
    Instant expiresAt) {}
