package com.tchalanet.server.features.cashier.offline;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesResult;
import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/offline")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Offline")
public class CashierOfflineController {

    private final CashierOfflineService service;

    @GetMapping("/grant/current")
    @Operation(summary = "Current offline grant for the seller / terminal / device")
    public ApiResponse<OfflineGrantView> currentGrant(
        @CurrentContext TchRequestContext ctx,
        @RequestParam @NotNull TerminalId terminalId,
        @RequestParam @NotNull UUID deviceId
    ) {
        return ApiResponse.success(service.currentGrant(ctx, terminalId, deviceId));
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload a batch of offline submissions for technical validation")
    public ApiResponse<SyncOfflineSalesResult> submitSubmissions(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CashierOfflineSyncRequest request
    ) {
        return ApiResponse.accepted(service.submit(ctx, request));
    }
}
