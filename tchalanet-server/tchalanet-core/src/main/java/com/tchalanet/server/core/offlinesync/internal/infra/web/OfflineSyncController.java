package com.tchalanet.server.core.offlinesync.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.internal.infra.web.mapper.OfflineSyncWebMapper;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.ReceiveOfflineBatchRequest;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.ReceiveOfflineBatchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/offline-sync")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class OfflineSyncController {

    private final CommandBus commandBus;
    private final OfflineSyncWebMapper mapper;

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasPermission('offlinesync.submit')")
    public ApiResponse<ReceiveOfflineBatchResponse> receiveBatch(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody ReceiveOfflineBatchRequest request) {
        var result = commandBus.execute(mapper.toCommand(ctx, request));
        return ApiResponse.success(mapper.toResponse(result));
    }
}
