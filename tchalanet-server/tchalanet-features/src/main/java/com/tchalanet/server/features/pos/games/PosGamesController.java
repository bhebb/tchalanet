package com.tchalanet.server.features.pos.games;

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
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Games")
public class PosGamesController {

    private final PosGamesService service;

    @GetMapping("/available")
    @Operation(summary = "List cashier game choices with seller-facing bet option labels")
    public ApiResponse<List<PosGameOptionResponse>> available() {
        return ApiResponse.success(service.listAvailable());
    }
}
