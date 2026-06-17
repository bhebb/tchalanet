package com.tchalanet.server.features.cashier.session;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.cashier.session.app.CashierSessionService;
import com.tchalanet.server.features.cashier.session.model.CashierSessionView;
import com.tchalanet.server.features.cashier.session.model.CloseCashierSessionRequest;
import com.tchalanet.server.features.cashier.session.model.OpenCashierSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/session")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Session")
public class CashierSessionController {

    private final CashierSessionService service;

    @GetMapping("/current")
    @Operation(summary = "Current open POS session for a terminal (204 if none)")
    public ResponseEntity<ApiResponse<CashierSessionView>> current(
        @CurrentContext TchRequestContext ctx,
        @RequestParam @NotNull TerminalId terminalId
    ) {
        var view = service.current(ctx, terminalId);
        if (view == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(view));
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a POS session")
    public ApiResponse<CashierSessionView> open(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody OpenCashierSessionRequest request
    ) {
        return ApiResponse.created(service.open(ctx, request));
    }

    @PostMapping("/close")
    @Operation(summary = "Close a POS session")
    public ApiResponse<CashierSessionView> close(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CloseCashierSessionRequest request
    ) {
        return ApiResponse.success(service.close(ctx, request));
    }
}
