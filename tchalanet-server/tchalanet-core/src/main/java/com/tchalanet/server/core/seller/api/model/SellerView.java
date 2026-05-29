package com.tchalanet.server.core.seller.api.model;

import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record SellerView(
    SellerId id,
    UserId userId,
    String code,
    String displayName,
    SellerStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
