package com.tchalanet.server.features.cashier.operationalcontext;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.cashier.operationalcontext.app.CashierOperationalContextService;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOpContextOptionsView;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOperationalContextView;
import com.tchalanet.server.features.cashier.operationalcontext.model.SelectCashierOperationalContextRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/operational-context")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Operational context")
public class CashierOperationalContextController {

    private final CashierOperationalContextService service;

    @GetMapping("/options")
    @Operation(summary = "Outlets and terminals available to the current seller. "
        + "When outlets.size==1 && terminals.size==1, mobile SHOULD auto-select without showing a picker.")
    public ApiResponse<CashierOpContextOptionsView> options(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.options(ctx));
    }

    @GetMapping("/current")
    @Operation(summary = "Operational context attached to the current request (204 if none)")
    public ResponseEntity<ApiResponse<CashierOperationalContextView>> current(
        @CurrentContext TchRequestContext ctx
    ) {
        var view = service.current(ctx);
        if (view == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(view));
    }

    @PostMapping("/select")
    @Operation(summary = "Validate the seller can use the requested (outlet, terminal, session)")
    public ApiResponse<CashierOperationalContextView> select(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody SelectCashierOperationalContextRequest request
    ) {
        return ApiResponse.success(service.select(ctx, request));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Acknowledge the seller has cleared their operational context")
    public void clear(@CurrentContext TchRequestContext ctx) {
        service.clear(ctx);
    }
}
