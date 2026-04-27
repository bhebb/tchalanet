package com.tchalanet.server.core.tenantconfig.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.tenantconfig.application.command.model.*;
import com.tchalanet.server.core.tenantconfig.application.query.model.GetTenantByCodeQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.ListTenantsQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.TenantConfigView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Platform admin controller for tenant management.
 * Per web_api.md + api_response.md:
 * - Uses ApiResponse wrapper
 * - Commands dispatched via CommandBus (VoidCommandHandler)
 * - Queries dispatched via QueryBus
 * - @Valid on request bodies
 */
@Tag(name = "Platform • Tenants")
@RestController
@RequestMapping("/platform/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "List all tenants with pagination")
  @GetMapping
  public ApiResponse<TchPage<TenantConfigView>> list(
      @TchPaging(
          allowedSort = {"createdAt", "code", "name", "status"},
          defaultSort = {"createdAt,desc"}
      ) TchPageRequest pageReq) {
    var result = queryBus.send(new ListTenantsQuery(pageReq.pageable()));
    return ApiResponse.success(result);
  }

  @Operation(summary = "Get tenant by ID")
  @GetMapping("/{id}")
  public ApiResponse<TenantConfigView> get(@PathVariable TenantId id) {
    var dto = queryBus.send(new GetTenantByIdQuery(id));
    return ApiResponse.success(dto);
  }

  @Operation(summary = "Get tenant by code")
  @GetMapping("/by-code")
  public ApiResponse<TenantConfigView> getByCode(@RequestParam("code") String code) {
    var tenant = queryBus.send(new GetTenantByCodeQuery(code));
    return ApiResponse.success(tenant);
  }

  @Operation(summary = "Create a new tenant")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void create(@Valid @RequestBody CreateTenantCommand cmd) {
    commandBus.send(cmd);
  }

  @Operation(summary = "Activate tenant (DRAFT → ACTIVE)")
  @PostMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@PathVariable TenantId id) {
    commandBus.send(new ActivateTenantCommand(id));
  }

  @Operation(summary = "Suspend tenant (ACTIVE → SUSPENDED)")
  @PostMapping("/{id}/suspend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void suspend(
      @PathVariable TenantId id,
      @RequestBody(required = false) ReasonRequest body) {
    var reason = body != null && body.reason() != null ? body.reason() : null;
    commandBus.send(new SuspendTenantCommand(id, reason));
  }

  @Operation(summary = "Deactivate tenant (ACTIVE → SUSPENDED)")
  @PostMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(
      @PathVariable TenantId id,
      @RequestBody(required = false) ReasonRequest body) {
    var reason = body != null && body.reason() != null ? body.reason() : null;
    commandBus.send(new DeactivateTenantCommand(id, reason));
  }

  @Operation(summary = "Archive tenant (any → ARCHIVED)")
  @PostMapping("/{id}/archive")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(
      @PathVariable TenantId id,
      @RequestBody(required = false) ReasonRequest body) {
    var reason = body != null && body.reason() != null ? body.reason() : null;
    commandBus.send(new ArchiveTenantCommand(id, reason));
  }

  /**
   * Request body for operations that accept an optional reason.
   */
  public record ReasonRequest(String reason) {}
}
