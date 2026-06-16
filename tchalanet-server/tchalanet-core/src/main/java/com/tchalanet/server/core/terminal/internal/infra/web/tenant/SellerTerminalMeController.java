package com.tchalanet.server.core.terminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalView;
import com.tchalanet.server.core.terminal.api.query.GetSellerTerminalMeQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/terminal")
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
@Tag(name = "Terminal POS • Me")
@RequiredArgsConstructor
public class SellerTerminalMeController {

    private final QueryBus queryBus;

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated seller terminal profile")
    public ApiResponse<SellerTerminalView> me(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(queryBus.ask(new GetSellerTerminalMeQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.sellerTerminalId())));
    }
}
