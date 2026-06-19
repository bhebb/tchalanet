package com.tchalanet.server.core.sales.internal.domain.model.preparation;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Working trace of a prepared sale (DOMAIN_SALES.md §11). The persisted ticket
 * stays the financial truth; this object pins the previewed lines so confirm
 * persists exactly what the seller saw.
 */
public record SalePreparation(
    UUID id,
    SalePreparationStatus status,
    SellerTerminalId sellerTerminalId,
    UUID drawId,
    String inputHash,
    Map<String, Object> input,
    UUID promotionDecisionId,
    String idempotencyKey,
    UUID ticketId,
    Instant expiresAt,
    Instant confirmedAt,
    List<SalePreparationPromotionLine> promotionLines
) {
    public boolean isExpired(Instant now) {
        return status == SalePreparationStatus.DRAFT && now.isAfter(expiresAt);
    }

    public Optional<SalePreparationPromotionLine> line(String lineRef) {
        return promotionLines.stream().filter(l -> l.lineRef().equals(lineRef)).findFirst();
    }
}
