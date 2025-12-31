package com.tchalanet.server.core.theme.infra.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import com.tchalanet.server.core.theme.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.core.theme.infra.persistence.ThemeJpaEntity;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RepositoryEventHandler(ThemeJpaEntity.class)
public class ThemeRepositoryEventHandler {

  private final JpaThemeRepository repo;

  public ThemeRepositoryEventHandler(JpaThemeRepository repo) {
    this.repo = repo;
  }

  @HandleBeforeCreate
  public void beforeCreate(ThemeJpaEntity entity, @CurrentContext TchRequestContext ctx) {
    UUID effectiveTenant = requireTenant(ctx);

    // Tenant safety
    if (entity.getTenantId() == null) {
      entity.setTenantId(effectiveTenant);
    } else if (!Objects.equals(entity.getTenantId(), effectiveTenant) && !isSuperAdmin(ctx)) {
      throw forbidden("Cross-tenant theme create is forbidden");
    }

    // Force DRAFT + themeVersion=0
    entity.setStatus(ThemeStatus.DRAFT);
    entity.setThemeVersion(0);

    // JSON defaults (avoid null jsonb)
    if (entity.getPalette() == null) entity.setPalette(java.util.Map.of());
    if (entity.getTokens() == null) entity.setTokens(java.util.Map.of());
    if (entity.getCssVars() == null) entity.setCssVars(java.util.Map.of());
  }

  @HandleBeforeSave
  public void beforeSave(ThemeJpaEntity incoming, @CurrentContext TchRequestContext ctx) {
    UUID effectiveTenant = requireTenant(ctx);

    // Load original to detect forbidden field changes + status rules
    ThemeJpaEntity original =
        repo.findById(incoming.getId())
            .orElseThrow(() -> notFound("Theme not found: " + incoming.getId()));

    // Tenant safety: cannot move theme to another tenant
    if (!Objects.equals(original.getTenantId(), incoming.getTenantId())) {
      throw conflict("tenantId is immutable");
    }
    // Normal users cannot update other tenant themes
    if (!Objects.equals(original.getTenantId(), effectiveTenant) && !isSuperAdmin(ctx)) {
      throw forbidden("Cross-tenant theme update is forbidden");
    }

    // Status rules
    if (original.getStatus() == ThemeStatus.ARCHIVED) {
      throw conflict("Archived theme cannot be modified");
    }

    boolean admin = isTenantAdmin(ctx) || isSuperAdmin(ctx);

    if (original.getStatus() == ThemeStatus.PUBLISHED && !admin) {
      throw forbidden("Only tenant admin or super admin can modify a published theme");
    }

    // Prevent REST from changing status/themeVersion (reserved for publish handler)
    incoming.setStatus(original.getStatus());
    incoming.setThemeVersion(original.getThemeVersion());

    // JSON defaults
    if (incoming.getPalette() == null) incoming.setPalette(java.util.Map.of());
    if (incoming.getTokens() == null) incoming.setTokens(java.util.Map.of());
    if (incoming.getCssVars() == null) incoming.setCssVars(java.util.Map.of());
  }

  @HandleBeforeDelete
  public void beforeDelete(ThemeJpaEntity entity, @CurrentContext TchRequestContext ctx) {
    // Always use ArchiveThemeCommand instead
    throw new ResponseStatusException(
        HttpStatus.METHOD_NOT_ALLOWED, "Theme deletion is not allowed. Use archive via command.");
  }

  private static UUID requireTenant(TchRequestContext ctx) {
    UUID t = ctx == null ? null : ctx.effectiveTenantUuid();
    if (t == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No effective tenant");
    return t;
  }

  private static boolean isSuperAdmin(TchRequestContext ctx) {
    return ctx != null
        && ctx.systemRoles() != null
        && ctx.systemRoles().contains(TchRole.SUPER_ADMIN);
  }

  private static boolean isTenantAdmin(TchRequestContext ctx) {
    return ctx != null
        && ctx.systemRoles() != null
        && ctx.systemRoles().contains(TchRole.TENANT_ADMIN);
  }

  private static ResponseStatusException forbidden(String msg) {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
  }

  private static ResponseStatusException conflict(String msg) {
    return new ResponseStatusException(HttpStatus.CONFLICT, msg);
  }

  private static ResponseStatusException notFound(String msg) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
  }
}
