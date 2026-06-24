package com.tchalanet.server.features.tenantadmin.tenant;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/admin/tenant-config")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Config")
public class AdminTenantConfigController {

  private final TenantConfigApi tenantConfig;
  private final JsonUtils jsonUtils;

  @GetMapping
  @Operation(summary = "Get internal settings of the current tenant")
  public ApiResponse<TenantInternalSettings> getConfig(@CurrentContext TchRequestContext ctx) {
    var view = tenantConfig.getTenantById(new GetTenantByIdRequest(ctx.tenantId()));
    var settings = jsonUtils.treeToValue(view.internalSettings(), TenantInternalSettings.class);
    return ApiResponse.success(settings);
  }

  @GetMapping("/communication")
  @Operation(summary = "Get communication config of the current tenant")
  public ApiResponse<TenantInternalCommunicationConfig> getCommunication(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(tenantConfig.getTenantCommunicationConfig(new GetTenantByIdRequest(ctx.tenantId())));
  }

  @GetMapping("/document")
  @Operation(summary = "Get document config of the current tenant")
  public ApiResponse<TenantInternalDocumentConfig> getDocument(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(tenantConfig.getTenantDocumentConfig(new GetTenantByIdRequest(ctx.tenantId())));
  }

  @PutMapping("/internal-settings")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update internal settings of the current tenant")
  public void updateSettings(
      @CurrentContext TchRequestContext ctx,
      @RequestBody JsonNode body) {
    tenantConfig.updateTenantInternalSettings(new UpdateTenantInternalSettingsRequest(ctx.tenantId(), body));
  }
}
