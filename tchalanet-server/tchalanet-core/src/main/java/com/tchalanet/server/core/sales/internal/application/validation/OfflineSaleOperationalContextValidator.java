package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.api.command.OfflineTicketSaleInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflineSaleOperationalContextValidator {

    private final QueryBus queryBus;

    // TODO(sales-refactor): restore offline operational context queries and typed return model.
    public void validateForOfflineSync(OfflineTicketSaleInput input) {
        // Intentionally no-op for now.
    }
}
