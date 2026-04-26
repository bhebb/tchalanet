package com.tchalanet.server.catalog.pagemodeltemplate.internal.web;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/page-model-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Platform • PageModel Template")
public class PlatformPageModelTemplateController {

    private final PageModelTemplateCatalog catalog;
    private final PageModelTemplateAdminService admin;

    @Operation(summary = "List templates visible under RLS")
    @GetMapping("/visible")
    public ApiResponse<java.util.List<PageModelTemplateView>> visible() {
        return ApiResponse.success(catalog.listVisible());
    }

    @Operation(summary = "Search templates (paged)")
    @GetMapping
    public ApiResponse<TchPage<PageModelTemplateView>> search(
        @RequestParam(required = false) String logicalIdContains,
        @RequestParam(required = false) String nameContains,
        @TchPaging(allowedSort = {"updatedAt","createdAt","logicalId","name"}, defaultSort = {"updatedAt,DESC"})
        TchPageRequest pageReq
    ) {
        return ApiResponse.success(catalog.search(logicalIdContains, nameContains, pageReq));
    }

    @Operation(summary = "Get by id")
    @GetMapping("/{id}")
    public ApiResponse<PageModelTemplateView> get(@PathVariable PageModelTemplateId id) {
        return ApiResponse.success(catalog.findById(id).orElse(null));
    }

    @Operation(summary = "Create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageModelTemplateView> create(@RequestBody PageModelTemplateView view) {
        return ApiResponse.success(admin.createFromView(view));
    }

    @Operation(summary = "Update")
    @PutMapping("/{id}")
    public ApiResponse<PageModelTemplateView> update(
        @PathVariable PageModelTemplateId id,
        @RequestBody PageModelTemplateView view,
        @CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(admin.updateFromView(id, view, ctx.userId()));
    }

    @Operation(summary = "Soft delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable PageModelTemplateId id) {
        admin.softDelete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Set default (optional)")
    @PostMapping("/{id}/default")
    public ApiResponse<PageModelTemplateView> setDefault(@PathVariable PageModelTemplateId id) {
        return ApiResponse.success(admin.setDefault(id));
    }

    @Operation(summary = "Preview a template")
    @GetMapping("/{id}/preview")
    public ApiResponse<PageModelTemplateView> preview(@PathVariable PageModelTemplateId id) {
        return ApiResponse.success(admin.preview(id));
    }

    @Operation(summary = "Duplicate a template")
    @PostMapping("/{id}/duplicate")
    public ApiResponse<PageModelTemplateView> duplicate(
        @PathVariable PageModelTemplateId id,
        @RequestParam(name = "logicalId", required = false) String newLogicalId,
        @RequestParam(name = "code", required = false) String newCode) {
        return ApiResponse.success(admin.duplicate(id, newLogicalId, newCode));
    }

    @Operation(summary = "Reset a template")
    @PostMapping("/{id}/reset")
    public ApiResponse<PageModelTemplateView> reset(@PathVariable PageModelTemplateId id) {
        return ApiResponse.success(admin.resetToDefaults(id));
    }
}
