package com.tchalanet.server.catalog.drawchannel.internal.web;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelFlagsRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelRequest;
import com.tchalanet.server.catalog.drawchannel.internal.write.DrawChannelAdminService;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/draw-channels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform • Draw Channels")
public class PlatformDrawChannelController {

    private final DrawChannelCatalog catalog;
    private final DrawChannelAdminService adminService;

    @Operation(summary = "Search draw channels (platform)")
    @GetMapping
    public ApiResponse<TchPage<DrawChannelView>> list(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String nameContains,
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @RequestParam(required = false) String externalProvider,
        @RequestParam(required = false) Boolean active,
        @TchPaging(allowedSort = {"createdAt", "updatedAt", "code", "name"}, defaultSort = {"createdAt,DESC"}) TchPageRequest pageReq,
        @CurrentContext TchRequestContext ctx) {

        TenantId tenantId = ctx.tenantIdSafe();
        DrawChannelSearchCriteria criteria = new DrawChannelSearchCriteria(
            tenantId,
            code,
            nameContains,
            resultSlotId,
            externalProvider,
            active
        );

        var page = catalog.search(criteria, pageReq);
        return ApiResponse.success(page);
    }

    @Operation(summary = "Get draw channel by id (platform)")
    @GetMapping("/{id}")
    public ApiResponse<DrawChannelView> get(@PathVariable DrawChannelId id, @CurrentContext TchRequestContext ctx) {
        var tenantId = ctx.tenantIdSafe();
        var opt = catalog.findById(tenantId, id);
        return ApiResponse.success(opt.orElse(null));
    }

    @Operation(summary = "Create a draw channel (platform)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DrawChannelView> create(@RequestBody CreateDrawChannelRequest req) {
        var view = adminService.createFromRequest(req);
        return ApiResponse.success(view);
    }

    @Operation(summary = "Update a draw channel (platform)")
    @PutMapping("/{id}")
    public ApiResponse<DrawChannelView> update(@PathVariable DrawChannelId id, @RequestBody UpdateDrawChannelRequest req) {
        var view = adminService.updateFromRequest(id, req);
        return ApiResponse.success(view);
    }

    @Operation(summary = "Patch flags for a draw channel (platform)")
    @PatchMapping("/{id}/flags")
    public ApiResponse<DrawChannelView> patchFlags(@PathVariable DrawChannelId id, @Valid @RequestBody UpdateDrawChannelFlagsRequest req) {
        var view = adminService.updateFlagsFromRequest(id, req);
        return ApiResponse.success(view);
    }

    @Operation(summary = "Soft-delete a draw channel (platform)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable DrawChannelId id) {
        adminService.softDelete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Disable a draw channel — kill switch (platform)")
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable DrawChannelId id) {
        adminService.disableChannel(id);
        return ApiResponse.success(null);
    }
}
