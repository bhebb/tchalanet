package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserRow;
import com.tchalanet.server.platform.identity.internal.service.UserAdminService;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.CreateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.SetUserRoleRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.TenantUserAdminResponse;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpdatePreferencesRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpdateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpsertMembershipRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/membership")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantUserAdminController {

  private final UserBootstrapService bootstrapService;
  private final CurrentUserProfileService profiles;
  private final UserAdminService users;
  private final TenantMembershipService memberships;

  @GetMapping("/bootstrap")
  public ApiResponse<TenantUserAdminResponse> bootstrap(@CurrentContext TchRequestContext ctx) {
    if (ctx.userId() == null) {
      if (ctx.keycloakUserId() == null) {
        throw ProblemRest.forbidden("Missing Keycloak subject (sub) in token");
      }
      var result =
          bootstrapService.bootstrap(
              new BootstrapCurrentUserRequest(
                  KeycloakUserSub.parse(ctx.keycloakUserId()),
                  ctx.effectiveTenantCode(),
                  ctx.keycloakUserId(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  ctx.locale(),
                  ctx.tenantZoneId()));
      return ApiResponse.success(loadAndMap(result.userId()));
    }
    return ApiResponse.success(loadAndMap(ctx.userId()));
  }

  @GetMapping
  public ApiResponse<TchPage<TenantUserRow>> list(
      @CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
    var page = memberships.list(ctx.tenantId(), pageReq);
    return ApiResponse.success(TchPageMapper.map(page, row -> row));
  }

  @GetMapping("/{userId}")
  public ApiResponse<TenantUserAdminResponse> getUser(
      @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
    return ApiResponse.success(loadAndMap(userId));
  }

  @PostMapping
  public ApiResponse<TenantUserAdminResponse> create(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateUserRequest req) {
    if (req.role() == null) {
      throw ProblemRest.badRequest("role is required");
    }
    if (req.role() == TchRole.CASHIER && req.outletId() == null) {
      throw ProblemRest.badRequest("outletId is required when role=CASHIER");
    }

    var created =
        users.createUser(
            req.email(),
            req.phone(),
            req.firstName(),
            req.lastName(),
            null,
            null,
            null,
            null,
            null,
            false,
            Set.of());
    memberships.assign(ctx.tenantId(), created.userId(), null, req.outletId(), req.terminalId(), false);
    memberships.setRole(ctx.tenantId(), created.userId(), req.role());
    return ApiResponse.success(loadAndMap(created.userId()));
  }

  @PutMapping("/{userId}")
  public ApiResponse<TenantUserAdminResponse> updateProfile(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @Valid @RequestBody UpdateUserRequest req) {
    profiles.updateProfile(
        new UpdateUserProfileRequest(
            userId,
            Optional.ofNullable(req.firstName()),
            Optional.ofNullable(req.lastName()),
            Optional.ofNullable(req.email()),
            Optional.ofNullable(req.phone()),
            Optional.empty()));
    return ApiResponse.success(loadAndMap(userId));
  }

  @PatchMapping("/{userId}/preferences")
  public ApiResponse<TenantUserAdminResponse> updatePreferences(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @Valid @RequestBody UpdatePreferencesRequest req) {
    profiles.updatePreferences(
        userId,
        req.themeMode(),
        req.density(),
        parseLocale(req.locale()).orElse(null),
        parseZone(req.timeZone()).orElse(null),
        parseCurrency(req.currency()).orElse(null));
    return ApiResponse.success(loadAndMap(userId));
  }

  @PutMapping("/{userId}/membership")
  public ApiResponse<TenantUserAdminResponse> upsertMembership(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @Valid @RequestBody UpsertMembershipRequest req) {
    memberships.assign(ctx.tenantId(), userId, null, req.outletId(), req.terminalId(), false);
    return ApiResponse.success(loadAndMap(userId));
  }

  @DeleteMapping("/{userId}/membership")
  public ApiResponse<TenantUserAdminResponse> deleteMembership(
      @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
    memberships.unassign(ctx.tenantId(), userId);
    return ApiResponse.success(loadAndMap(userId));
  }

  @PutMapping("/{userId}/role")
  public ApiResponse<TenantUserAdminResponse> setRole(
      @CurrentContext TchRequestContext ctx,
      @PathVariable UserId userId,
      @Valid @RequestBody SetUserRoleRequest req) {
    if (req.role() == null) {
      throw ProblemRest.badRequest("role is required");
    }
    memberships.setRole(ctx.tenantId(), userId, req.role());
    return ApiResponse.success(loadAndMap(userId));
  }

  private TenantUserAdminResponse loadAndMap(UserId userId) {
    var profile = profiles.getUserProfile(userId);
    return new TenantUserAdminResponse(
        profile.id(),
        profile.keycloakSub() == null ? null : profile.keycloakSub().value().toString(),
        profile.username(),
        profile.email(),
        profile.firstName(),
        profile.lastName(),
        profile.displayName());
  }

  private static Optional<Locale> parseLocale(String locale) {
    if (locale == null || locale.isBlank()) return Optional.empty();
    try {
      return Optional.of(Locale.forLanguageTag(locale));
    } catch (Exception e) {
      throw ProblemRest.badRequest("Invalid locale: " + locale);
    }
  }

  private static Optional<ZoneId> parseZone(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) return Optional.empty();
    try {
      return Optional.of(ZoneId.of(timeZone));
    } catch (Exception e) {
      throw ProblemRest.badRequest("Invalid timeZone: " + timeZone);
    }
  }

  private static Optional<Currency> parseCurrency(String currency) {
    if (currency == null || currency.isBlank()) return Optional.empty();
    try {
      return Optional.of(Currency.getInstance(currency.toUpperCase()));
    } catch (Exception e) {
      throw ProblemRest.badRequest("Invalid currency: " + currency);
    }
  }
}
