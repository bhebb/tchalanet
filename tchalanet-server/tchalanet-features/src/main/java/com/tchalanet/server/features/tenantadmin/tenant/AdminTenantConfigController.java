package com.tchalanet.server.features.tenantadmin.tenant;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantInternalSettingsSectionRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsSectionRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantHolidayTemplateView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/admin/tenant-config")
@PreAuthorize("hasPermission(null, 'tenant.config.read')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Config")
public class AdminTenantConfigController {

  private final TenantConfigApi tenantConfig;

  @GetMapping
  @Operation(summary = "Get internal settings of the current tenant")
  public ApiResponse<TenantInternalSettings> getConfig(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(
        tenantConfig.getTenantInternalSettings(new GetTenantByIdRequest(ctx.tenantIdRequired())));
  }

  @GetMapping("/communication")
  @Operation(summary = "Get communication config of the current tenant")
  public ApiResponse<TenantInternalCommunicationConfig> getCommunication(
      @CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(
        tenantConfig.getTenantCommunicationConfig(
            new GetTenantByIdRequest(ctx.tenantIdRequired())));
  }

  @GetMapping("/document")
  @Operation(summary = "Get document config of the current tenant")
  public ApiResponse<TenantInternalDocumentConfig> getDocument(
      @CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(
        tenantConfig.getTenantDocumentConfig(new GetTenantByIdRequest(ctx.tenantIdRequired())));
  }

  @GetMapping("/readiness")
  @Operation(summary = "Get tenant settings readiness of the current tenant")
  public ApiResponse<TenantSettingsReadinessView> getReadiness(
      @CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(
        tenantConfig.getTenantSettingsReadiness(new GetTenantByIdRequest(ctx.tenantIdRequired())));
  }

  @GetMapping("/holiday-templates")
  @Operation(summary = "List selectable tenant holiday templates")
  public ApiResponse<List<TenantHolidayTemplateView>> getHolidayTemplates() {
    return ApiResponse.success(tenantConfig.listTenantHolidayTemplates());
  }

  @GetMapping("/sections/{section}")
  @Operation(summary = "Get one effective internal settings section of the current tenant")
  public ApiResponse<JsonNode> getSection(
      @CurrentContext TchRequestContext ctx, @PathVariable String section) {
    return ApiResponse.success(
        tenantConfig.getTenantInternalSettingsSection(
            new GetTenantInternalSettingsSectionRequest(
                ctx.tenantIdRequired(),
                UpdateTenantInternalSettingsSectionRequest.Section.fromJsonKey(section))));
  }

  @PutMapping("/internal-settings")
  @PreAuthorize("hasPermission(null, 'tenant.config.manage')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update internal settings of the current tenant")
  public void updateSettings(@CurrentContext TchRequestContext ctx, @RequestBody JsonNode body) {
    tenantConfig.updateTenantInternalSettings(
        new UpdateTenantInternalSettingsRequest(ctx.tenantIdRequired(), body));
  }

  @PutMapping("/sections/{section}")
  @PreAuthorize("hasPermission(null, 'tenant.config.manage')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update one internal settings section of the current tenant")
  public void updateSettingsSection(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String section,
      @RequestBody JsonNode body) {
    tenantConfig.updateTenantInternalSettingsSection(
        new UpdateTenantInternalSettingsSectionRequest(
            ctx.tenantIdRequired(),
            UpdateTenantInternalSettingsSectionRequest.Section.fromJsonKey(section),
            body));
  }
}
