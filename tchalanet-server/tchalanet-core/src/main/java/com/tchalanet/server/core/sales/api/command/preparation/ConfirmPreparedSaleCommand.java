package com.tchalanet.server.core.sales.api.command.preparation;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.api.model.preparation.ConfirmPreparedSaleResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Confirms a prepared sale. Payload = preparationId + idempotencyKey only —
 * the client never sends lines; the preparation is the single source of truth
 * for the previewed selections. Double confirm with the same idempotencyKey
 * returns the same ticket.
 */
public record ConfirmPreparedSaleCommand(
    @NotNull UUID preparationId,
    @NotBlank String idempotencyKey
) implements Command<ConfirmPreparedSaleResult> {}
