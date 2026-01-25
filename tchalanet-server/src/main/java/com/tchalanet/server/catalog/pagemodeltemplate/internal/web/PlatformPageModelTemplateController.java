package com.tchalanet.server.catalog.pagemodeltemplate.internal.web;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/page-model-templates")
@RequiredArgsConstructor
@Tag(name = "Platform • PageModel Template")
public class PlatformPageModelTemplateController {

  private final PageModelTemplateAdminService adminService;

  @Operation(summary = "Get a template by id (platform admin)")
  @GetMapping("/{id}")
  public ApiResponse<PageModelTemplateView> getById(@PathVariable PageModelTemplateId id) {
    return ApiResponse.success(adminService.findViewById(id).orElse(null));
  }

  @Operation(summary = "Create a template (platform admin)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PageModelTemplateView> create(@RequestBody PageModelTemplateView view) {
    var response = adminService.createFromView(view);
    return ApiResponse.success(response);
  }

  @Operation(summary = "Update a template (platform admin)")
  @PutMapping("/{id}")
  public ApiResponse<PageModelTemplateView> update(
      @PathVariable PageModelTemplateId id, @RequestBody PageModelTemplateView view) {
    var response = adminService.updateFromView(id, view, null);
    return ApiResponse.success(response);
  }

  @Operation(summary = "Soft-delete a template (platform admin)")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable PageModelTemplateId id) {
    adminService.softDelete(id.value(), null);
    return ApiResponse.success(null);
  }
}
