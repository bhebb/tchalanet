package com.tchalanet.server.core.sales.internal.domain.service.preparation;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationTransition;
import org.springframework.stereotype.Component;

/**
 * DRAFT -> CONFIRMED | EXPIRED | CANCELLED ; terminal states accept nothing.
 */
@Component
public final class SalePreparationStateMachine {

    public SalePreparationStatus apply(
        SalePreparationStatus current,
        SalePreparationTransition transition
    ) {
        if (current == null) {
            throw ProblemRest.conflict("sales.preparation.status_missing");
        }
        if (current != SalePreparationStatus.DRAFT) {
            throw switch (current) {
                case CONFIRMED -> ProblemRest.conflict("sales.preparation.already_confirmed");
                case EXPIRED -> ProblemRest.conflict("sales.preparation.expired");
                case CANCELLED -> ProblemRest.conflict("sales.preparation.cancelled");
                case DRAFT -> new IllegalStateException("unreachable");
            };
        }
        return switch (transition) {
            case CONFIRM -> SalePreparationStatus.CONFIRMED;
            case EXPIRE -> SalePreparationStatus.EXPIRED;
            case CANCEL -> SalePreparationStatus.CANCELLED;
        };
    }
}
