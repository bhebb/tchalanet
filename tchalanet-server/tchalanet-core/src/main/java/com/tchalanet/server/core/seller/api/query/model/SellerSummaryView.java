package com.tchalanet.server.core.seller.api.query.model;

import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record SellerSummaryView(
    SellerId id,
    UserId userId,
    String code,
    String displayName,
    String status,
    Instant createdAt
) {}
