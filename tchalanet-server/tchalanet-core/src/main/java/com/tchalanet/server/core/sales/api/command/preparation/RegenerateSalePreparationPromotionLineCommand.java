package com.tchalanet.server.core.sales.api.command.preparation;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Regenerates the selection of one generated promotion line, before confirm
 * only: DRAFT non expirée, ligne promotionnelle régénérable, compteur
 * sous {@code maxRegenerationsBeforeConfirm}. Chaque régénération est auditée.
 */
public record RegenerateSalePreparationPromotionLineCommand(
    @NotNull UUID preparationId,
    @NotBlank String lineRef
) implements Command<SalePreparationView> {}
