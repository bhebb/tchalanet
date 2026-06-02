package com.tchalanet.server.platform.tenant.internal.web;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.tenant.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ArchiveTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenant.internal.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Platform - Tenants")
@RestController
@RequestMapping("/platform/tenants")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminController {

  private final TenantConfigService tenants;

  @Operation(summary = "List all tenants with pagination")
  @GetMapping
  public ApiResponse<TchPage<TenantConfigView>> list(
      @TchPaging(allowedSort = {"createdAt", "code", "name", "status"}, defaultSort = {"createdAt,desc"})
          TchPageRequest pageReq) {
    return ApiResponse.success(tenants.listTenants(new ListTenantsRequest(pageReq.pageable())));
  }

  @GetMapping("/{id}")
  public ApiResponse<TenantConfigView> get(@PathVariable TenantId id) {
    return ApiResponse.success(tenants.getTenantById(new GetTenantByIdRequest(id)));
  }

  @GetMapping("/by-code")
  public ApiResponse<TenantConfigView> getByCode(@RequestParam("code") String code) {
    return ApiResponse.success(tenants.getTenantByCode(new GetTenantByCodeRequest(code)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void create(@Valid @RequestBody CreateTenantRequest request) {
    tenants.createTenant(request);
  }

  @PostMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@PathVariable TenantId id) {
    tenants.activateTenant(new ActivateTenantRequest(id));
  }

  @PostMapping("/{id}/suspend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void suspend(@PathVariable TenantId id, @RequestBody(required = false) ReasonRequest body) {
    tenants.suspendTenant(new SuspendTenantRequest(id, body == null ? null : body.reason()));
  }

  @PostMapping("/{id}/archive")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable TenantId id, @RequestBody(required = false) ReasonRequest body) {
    tenants.archiveTenant(new ArchiveTenantRequest(id, body == null ? null : body.reason()));
  }

  public record ReasonRequest(String reason) {}
}
