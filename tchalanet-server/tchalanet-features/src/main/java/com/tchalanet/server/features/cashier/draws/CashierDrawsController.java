package com.tchalanet.server.features.cashier.draws;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Draws")
public class CashierDrawsController {

    private final CashierDrawsService service;

    @GetMapping("/available")
    @Operation(summary = "List draws available to the cashier for sale (next N hours)")
    public ApiResponse<List<CashierAvailableDrawView>> available(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(defaultValue = "24") int lookaheadHours,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(service.listAvailable(ctx, lookaheadHours, limit));
    }
}
