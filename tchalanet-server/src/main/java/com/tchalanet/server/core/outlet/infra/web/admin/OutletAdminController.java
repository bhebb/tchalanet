package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.outlet.application.command.model.CreateOutletCommand;
import com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class OutletAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public ApiResponse<List<OutletView>> list(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(queryBus.send(new ListOutletsByTenantQuery(ctx.tenantIdSafe())));
    }

    @GetMapping("/{id}")
    public ApiResponse<OutletView> get(@CurrentContext TchRequestContext ctx, @PathVariable OutletId id) {
        return ApiResponse.success(queryBus.send(new GetOutletByIdQuery(ctx.tenantIdSafe(), id)));
    }

    public record CreateOutletRequest(String name, String slug, AddressInput address) {}

    @PostMapping
    public ApiResponse<OutletId> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateOutletRequest req) {
        return ApiResponse.success(commandBus.send(new CreateOutletCommand(ctx.tenantIdSafe(), req.name(), req.slug(), null, req.address())));
    }

    @PatchMapping("/{id}/config")
    public ApiResponse<Void> updateConfig(@CurrentContext TchRequestContext ctx, @PathVariable OutletId id, @RequestBody OutletConfigPatch patch) {
        commandBus.send(new UpdateOutletConfigCommand(ctx.tenantIdSafe(), id, patch));
        return ApiResponse.success(null);
    }
}
