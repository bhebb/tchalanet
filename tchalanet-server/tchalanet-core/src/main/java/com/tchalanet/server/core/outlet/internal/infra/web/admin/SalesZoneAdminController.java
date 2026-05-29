package com.tchalanet.server.core.outlet.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.outlet.api.command.zone.CreateSalesZoneCommand;
import com.tchalanet.server.core.outlet.api.command.zone.UpdateSalesZoneCommand;
import com.tchalanet.server.core.outlet.api.query.GetSalesZoneQuery;
import com.tchalanet.server.core.outlet.api.query.ListSalesZonesQuery;
import com.tchalanet.server.core.outlet.api.query.SalesZoneView;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.CreateSalesZoneRequest;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.SalesZoneResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.UpdateSalesZoneRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/sales-zones")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Sales Zone • Admin")
public class SalesZoneAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping
    public ApiResponse<SalesZoneId> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CreateSalesZoneRequest req) {
        SalesZoneId id = commandBus.execute(
            new CreateSalesZoneCommand(
                ctx.tenantIdSafe(), req.code(), req.label(), req.parentId()));
        return ApiResponse.success(id);
    }

    @GetMapping
    public ApiResponse<List<SalesZoneResponse>> list(@CurrentContext TchRequestContext ctx) {
        var views = queryBus.ask(new ListSalesZonesQuery(ctx.tenantIdSafe()));
        return ApiResponse.success(views.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesZoneResponse> get(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SalesZoneId id) {
        var view = queryBus.ask(new GetSalesZoneQuery(ctx.tenantIdSafe(), id));
        return ApiResponse.success(toResponse(view));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> update(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SalesZoneId id,
        @Valid @RequestBody UpdateSalesZoneRequest req) {
        commandBus.execute(
            new UpdateSalesZoneCommand(ctx.tenantIdSafe(), id, req.label(), req.active()));
        return ApiResponse.success(null);
    }

    private SalesZoneResponse toResponse(SalesZoneView view) {
        return new SalesZoneResponse(
            view.id(),
            view.tenantId(),
            view.code(),
            view.label(),
            view.active(),
            view.parentId());
    }
}
