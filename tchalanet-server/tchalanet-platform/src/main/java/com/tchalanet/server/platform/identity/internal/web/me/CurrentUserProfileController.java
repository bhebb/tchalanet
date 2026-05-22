package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurfacePolicy;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapService;
import com.tchalanet.server.platform.identity.internal.web.model.EffectiveUiContextResponse;
import com.tchalanet.server.platform.identity.internal.web.model.LandingResponse;
import com.tchalanet.server.platform.identity.internal.web.model.MeResponse;
import com.tchalanet.server.platform.identity.internal.web.model.ProfileActionsResponse;
import com.tchalanet.server.platform.identity.internal.web.model.TenantContextResponse;
import com.tchalanet.server.platform.identity.internal.web.model.UserPreferenceResponse;
import com.tchalanet.server.platform.identity.internal.web.model.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/me/profile")
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'OPERATOR')")
@RequiredArgsConstructor
@Tag(name = "Tenant • Current Profile")
public class CurrentUserProfileController {

  private final CurrentUserProfileService profiles;
  private final UserBootstrapService bootstrapService;

  @GetMapping
  @Operation(summary = "Get current user profile")
  public ApiResponse<MeResponse> me(@CurrentContext TchRequestContext ctx) {
    if (ctx.userId() == null) {
      throw ProblemRest.notFound("User not found for current principal");
    }
    return ApiResponse.success(toMeResponse(profiles.getCurrentUser(ctx.userId()), ctx, false));
  }

  @PostMapping("/bootstrap")
  @Operation(summary = "Bootstrap current user profile from Keycloak sub")
  public ApiResponse<MeResponse> bootstrap(@CurrentContext TchRequestContext ctx) {
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
    return ApiResponse.success(toMeResponse(profiles.getCurrentUser(result.userId()), ctx, result.isNew()));
  }

  @PatchMapping
  @Operation(summary = "Patch current user profile")
  public ApiResponse<UserResponse> updateProfile(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody com.tchalanet.server.platform.identity.internal.web.model.UpdateUserProfileRequest req) {
    if (ctx.userId() == null) {
      throw ProblemRest.notFound("User not found for current principal");
    }
    profiles.updateProfile(
        new UpdateUserProfileRequest(
            ctx.userId(),
            Optional.ofNullable(req.firstName()),
            Optional.ofNullable(req.lastName()),
            Optional.empty(),
            Optional.ofNullable(req.phone()),
            Optional.ofNullable(req.locale()).map(Locale::forLanguageTag)));
    var profile = profiles.getUserProfile(ctx.userId());
    return ApiResponse.success(
        new UserResponse(
            profile.id(),
            profile.keycloakSub(),
            profile.username(),
            profile.email(),
            profile.firstName(),
            profile.lastName(),
            profile.displayName()));
  }

  private static MeResponse toMeResponse(CurrentUserView view, TchRequestContext ctx, boolean isNew) {
    var roles =
        ctx.systemRoles() == null
            ? Set.<TchRole>of()
            : ctx.systemRoles();
    var availableSurfaces = ClientSurfacePolicy.availableSurfaces(roles);
    return new MeResponse(
        view.id(),
        view.keycloakSub().value(),
        view.username(),
        view.email(),
        view.firstName(),
        view.lastName(),
        view.displayName(),
        isNew,
        new TenantContextResponse(
            view.tenantId(), view.tenantCode(), view.tenantTimeZone(), view.tenantCurrency()),
        new UserPreferenceResponse(
            view.themeMode(), view.density(), view.locale(), view.timeZone(), view.currency()),
        new EffectiveUiContextResponse(
            view.themeMode(), view.density(), view.locale(), view.timeZone(), view.currency()),
        roles,
        new LandingResponse(ClientSurfacePolicy.preferredSurface(roles), availableSurfaces),
        capabilities(availableSurfaces),
        new ProfileActionsResponse(true, true, false, true));
  }

  private static Set<String> capabilities(Set<ClientSurface> surfaces) {
    if (surfaces.contains(ClientSurface.MOBILE_POS)
        || surfaces.contains(ClientSurface.CASHIER_WEB)) {
      return Set.of("cashier.sell", "cashier.print", "cashier.send", "cashier.cancel");
    }
    return Set.of();
  }
}
