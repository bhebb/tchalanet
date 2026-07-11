package com.tchalanet.server.core.sales.internal.domain.service.preparation;

import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationTransition;

/**
 * DRAFT -> CONFIRMED | EXPIRED | CANCELLED ; terminal states accept nothing.
 */
public final class SalePreparationStateMachine {

    public SalePreparationStatus apply(
        SalePreparationStatus current,
        SalePreparationTransition transition
    ) {
        if (current == null) {
            throw conflict("sales.preparation.status_missing");
        }
        if (current != SalePreparationStatus.DRAFT) {
            throw switch (current) {
                case CONFIRMED -> conflict("sales.preparation.already_confirmed");
                case EXPIRED -> conflict("sales.preparation.expired");
                case CANCELLED -> conflict("sales.preparation.cancelled");
                case DRAFT -> new IllegalStateException("unreachable");
            };
        }
        return switch (transition) {
            case CONFIRM -> SalePreparationStatus.CONFIRMED;
            case EXPIRE -> SalePreparationStatus.EXPIRED;
            case CANCEL -> SalePreparationStatus.CANCELLED;
        };
    }

    private static TchConflictException conflict(String code) {
        return new TchConflictException(code, code);
    }
}
