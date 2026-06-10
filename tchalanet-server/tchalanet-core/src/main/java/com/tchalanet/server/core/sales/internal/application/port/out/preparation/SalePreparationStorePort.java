package com.tchalanet.server.core.sales.internal.application.port.out.preparation;

import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SalePreparationStorePort {

    SalePreparation create(SalePreparation preparation);

    Optional<SalePreparation> findById(UUID preparationId);

    void updateStatus(UUID preparationId, SalePreparationStatus status);

    void updateLineSelection(UUID preparationId, String lineRef, String selection, int regenerationCount);

    void confirm(UUID preparationId, UUID ticketId, String idempotencyKey, Instant confirmedAt);
}
