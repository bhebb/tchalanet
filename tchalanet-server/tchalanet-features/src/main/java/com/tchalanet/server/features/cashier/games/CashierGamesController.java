package com.tchalanet.server.features.cashier.games;

import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/games")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Games")
public class CashierGamesController {

    private final CashierGamesService service;

    @GetMapping("/available")
    @Operation(summary = "List cashier game choices with seller-facing bet option labels")
    public ApiResponse<List<CashierGameOptionResponse>> available() {
        return ApiResponse.success(service.listAvailable());
    }
}
