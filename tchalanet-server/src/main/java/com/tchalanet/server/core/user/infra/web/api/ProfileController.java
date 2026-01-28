package com.tchalanet.server.core.user.infra.web.api;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.query.model.CurrentUserDetails;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import com.tchalanet.server.core.user.infra.web.model.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController("profileApiController")
@RequestMapping("/admin/profile")
public class ProfileController {

    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public ProfileController(QueryBus queryBus, CommandBus commandBus) {
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    /**
     * Read current user profile. Assumes user is already present in DB.
     * <p>
     * If you decide to move "ensure user exists" into the query handler, this endpoint becomes the
     * single endpoint used at app start (no /bootstrap needed).
     */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@CurrentContext TchRequestContext ctx) {
        if (ctx.userId() == null) {
            // You can choose 409 with a dedicated problem type if you want:
            // throw ProblemRest.conflict("USER_NOT_BOOTSTRAPPED", "Call /bootstrap first");
            throw ProblemRest.notFound("User not found for current principal");
        }

        var details = queryBus.send(new GetCurrentUserQuery(ctx.userId()));
        if (details == null) {
            throw ProblemRest.notFound("User not found: " + ctx.userId());
        }

        return ApiResponse.success(toMeResponse(details, /*isNew*/ false));
    }

    /**
     * Ensure user exists in DB for current Keycloak principal (rare path). Returns the same payload as /me.
     * <p>
     * This is the only endpoint you need at app start IF you keep the "ensure" as an explicit action.
     */
    @PostMapping("/bootstrap")
    public ApiResponse<MeResponse> bootstrap(@CurrentContext TchRequestContext ctx) {
        if (ctx.keycloakUserId() == null) {
            throw ProblemRest.forbidden("Missing Keycloak subject (sub) in token");
        }

        var cmd =
            new EnsureUserExistsForPrincipalCommand(
                KeycloakUserSub.parse(ctx.keycloakUserId()),
                ctx.effectiveTenantCode(),
                ctx.keycloakUserId(), // username fallback
                null, // email unknown here (we can fetch from Keycloak in the handler if missing)
                null,
                null,
                null,
                null,
                ctx.locale(), // request locale (or null)
                ctx.tenantZoneId() // tenant default zoneId (or null)
            );

        var result = commandBus.send(cmd);

        var details = queryBus.send(new GetCurrentUserQuery(result.userId()));
        if (details == null) {
            throw ProblemRest.internal("Unable to load user after bootstrap: " + result.userId());
        }

        return ApiResponse.success(toMeResponse(details, result.isNew()));
    }

    /**
     * Update profile fields (not preferences).
     * Preferences should have their own endpoint if you want clean separation.
     */
    @PatchMapping
    public ApiResponse<UserResponse> updateProfile(
        @CurrentContext TchRequestContext ctx, @RequestBody UpdateUserProfileRequest req) {

        if (ctx.userId() == null) {
            throw ProblemRest.notFound("User not found for current principal");
        }

        var cmd =
            new UpdateUserProfileCommand(
                ctx.userId(),
                Optional.ofNullable(req.firstName()),
                Optional.ofNullable(req.lastName()),
                Optional.ofNullable(req.email()),
                Optional.ofNullable(req.locale())
                    .map(java.util.Locale::forLanguageTag));

        commandBus.send(cmd);

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(ctx.userId()));
        if (details == null) {
            throw ProblemRest.internal("Unable to load user after update: " + ctx.userId());
        }

        return ApiResponse.success(
            new UserResponse(
                details.id(),
                details.keycloakSub(),
                details.username(),
                details.email(),
                details.firstName(),
                details.lastName(),
                details.displayName()));
    }

    // ---------------------------------------------------------------------
    // Web mapping (trivial => OK inside controller)
    // If it grows: move to core.user.infra.web.mapper.UserWebMapper
    // ---------------------------------------------------------------------

    private MeResponse toMeResponse(CurrentUserDetails d, boolean isNew) {
        var tenant =
            new TenantContextResponse(
                d.tenant().tenantId(),
                d.tenant().tenantCode(),
                d.tenant().timeZone(),
                d.tenant().currency());

        var prefs =
            new UserPreferenceResponse(
                d.preferences().themeMode(),
                d.preferences().density(),
                d.preferences().locale(),
                d.preferences().timeZone(),
                d.preferences().currency());

        var effective =
            new EffectiveUiContextResponse(
                d.effective().themeMode(),
                d.effective().density(),
                d.effective().locale(),
                d.effective().timeZone(),
                d.effective().currency());

        return new MeResponse(
            d.id(),
            d.keycloakSub().value(),
            d.username(),
            d.email(),
            d.firstName(),
            d.lastName(),
            d.displayName(),
            isNew,
            tenant,
            prefs,
            effective);
    }
}
