package com.tchalanet.server.features.i18n;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.persistence.I18nOverrideEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/i18n-overrides")
@RequiredArgsConstructor
@Tags({@Tag(name = "Admin • I18n Overrides")})
public class TenantI18nOverrideController {

  private final TenantI18nOverrideService service;

  @Operation(summary = "List i18n overrides for current tenant")
  @GetMapping
  public Page<I18nOverrideEntity> listAllForTenant(
      @PageableDefault(size = 20, sort = "i18nKey") Pageable pageable,
      @CurrentContext TchRequestContext context) {

    return service.pageByTenant(context.tenantUuid(), pageable);
  }

  @Operation(summary = "List i18n overrides for tenant + locale")
  @GetMapping("/{locale}")
  public Page<I18nOverrideEntity> listForTenantAndLocale(
      @PathVariable String locale,
      @PageableDefault(size = 20, sort = "i18nKey") Pageable pageable,
      @CurrentContext TchRequestContext context) {
    return service.pageByTenantAndLocale(context.tenantUuid(), locale, pageable);
  }
}
