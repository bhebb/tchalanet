package com.tchalanet.server.core.sellerterminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sellerterminal.api.command.ChangeSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalMeQuery;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/tenant/seller-terminal", "/tenant/terminal"})
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
@Tag(name = "Terminal POS • Me")
@RequiredArgsConstructor
public class SellerTerminalMeController {

    private final QueryBus queryBus;
    private final CommandBus commandBus;

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated seller terminal profile")
    public ApiResponse<SellerTerminalView> me(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(queryBus.ask(new GetSellerTerminalMeQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.sellerTerminalIdRequired())));
    }

    @PostMapping("/me/change-pin")
    @Operation(summary = "Change the PIN of the current seller terminal")
    @AuditLog(
        entity = AuditEntityType.SELLER_TERMINAL,
        action = AuditAction.SELLER_TERMINAL_PIN_CHANGE,
        idExpression = "#ctx.sellerTerminalId().value().toString()")
    public void changePin(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody ChangePinRequest request
    ) {
        commandBus.execute(new ChangeSellerTerminalPinCommand(
            ctx.effectiveTenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            request.newPin()));
    }

    public record ChangePinRequest(
        @Pattern(regexp = "\\d{6}", message = "PIN must be exactly 6 digits")
        String newPin) {}
}
