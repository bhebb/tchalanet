package com.tchalanet.server.core.tenantuser.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.accesscontrol.application.command.model.SetTenantUserRoleCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.UnassignUserFromTenantCommand;
import com.tchalanet.server.core.tenantuser.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow;
import com.tchalanet.server.core.tenantuser.infra.web.admin.model.*;
import com.tchalanet.server.core.user.application.command.model.*;
import com.tchalanet.server.core.user.application.query.model.CurrentUserDetails;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/admin/membership")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantUserAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping("/bootstrap")
    public ApiResponse<TenantUserAdminResponse> bootstrap(@CurrentContext TchRequestContext ctx) {
        if (ctx.userId() == null) {
            if (ctx.keycloakUserId() == null) throw ProblemRest.forbidden("Missing Keycloak subject (sub) in token");
            var cmd = new EnsureUserExistsForPrincipalCommand(
                KeycloakUserSub.parse(ctx.keycloakUserId()),
                ctx.effectiveTenantCode(),
                ctx.keycloakUserId(),
                null, null, null, null, null,
                ctx.locale(), ctx.tenantZoneId());
            EnsureUserExistsForPrincipalResult res = commandBus.send(cmd);
            return ApiResponse.success(loadAndMap(res.userId()));
        }
        return ApiResponse.success(loadAndMap(ctx.userId()));
    }

    @GetMapping
    public ApiResponse<TchPage<TenantUserRow>> list(@CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
        var page = queryBus.send(new PagedListTenantUsersQuery(ctx.tenantId(), pageReq));
        return ApiResponse.success(TchPageMapper.map(page, r -> r));
    }

    @GetMapping("/{userId}")
    public ApiResponse<TenantUserAdminResponse> getUser(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        return ApiResponse.success(loadAndMap(userId));
    }

    @PostMapping
    public ApiResponse<TenantUserAdminResponse> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateUserRequest req) {
        if (req.role() == null) throw ProblemRest.badRequest("role is required");
        if (req.role() == TchRole.CASHIER && req.outletId() == null) {
            throw ProblemRest.badRequest("outletId is required when role=CASHIER");
        }

        var createCmd = new CreateUserCommand(
            req.email(), req.phone(), req.firstName(), req.lastName(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            false, Set.of());
        CreateUserResult createRes = commandBus.send(createCmd);

        commandBus.send(new AssignUserToTenantCommand(ctx.tenantId(), createRes.userId(), null, req.outletId(), req.terminalId(), false));
        commandBus.send(new SetTenantUserRoleCommand(ctx.tenantId(), createRes.userId(), req.role()));

        return ApiResponse.success(loadAndMap(createRes.userId()));
    }

    @PutMapping("/{userId}")
    public ApiResponse<TenantUserAdminResponse> updateProfile(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId, @Valid @RequestBody UpdateUserRequest req) {
        commandBus.send(new UpdateUserProfileCommand(
            userId,
            Optional.ofNullable(req.firstName()),
            Optional.ofNullable(req.lastName()),
            Optional.ofNullable(req.email()),
            Optional.ofNullable(req.phone()),
            Optional.empty()));
        return ApiResponse.success(loadAndMap(userId));
    }

    @PatchMapping("/{userId}/preferences")
    public ApiResponse<TenantUserAdminResponse> updatePreferences(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId, @Valid @RequestBody UpdatePreferencesRequest req) {
        commandBus.send(new UpdateUserPreferencesCommand(
            userId,
            Optional.ofNullable(req.themeMode()),
            Optional.ofNullable(req.density()),
            parseLocale(req.locale()),
            parseZone(req.timeZone()),
            parseCurrency(req.currency())));
        return ApiResponse.success(loadAndMap(userId));
    }

    @PutMapping("/{userId}/membership")
    public ApiResponse<TenantUserAdminResponse> upsertMembership(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId, @Valid @RequestBody UpsertMembershipRequest req) {
        commandBus.send(new AssignUserToTenantCommand(ctx.tenantId(), userId, null, req.outletId(), req.terminalId(), false));
        return ApiResponse.success(loadAndMap(userId));
    }

    @DeleteMapping("/{userId}/membership")
    public ApiResponse<TenantUserAdminResponse> deleteMembership(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        commandBus.send(new UnassignUserFromTenantCommand(ctx.tenantId(), userId));
        return ApiResponse.success(loadAndMap(userId));
    }

    @PutMapping("/{userId}/role")
    public ApiResponse<TenantUserAdminResponse> setRole(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId, @Valid @RequestBody SetUserRoleRequest req) {
        if (req.role() == null) throw ProblemRest.badRequest("role is required");
        commandBus.send(new SetTenantUserRoleCommand(ctx.tenantId(), userId, req.role()));
        return ApiResponse.success(loadAndMap(userId));
    }

    private TenantUserAdminResponse loadAndMap(UserId userId) {
        CurrentUserDetails d = queryBus.send(new GetCurrentUserQuery(userId));
        if (d == null) throw ProblemRest.notFound("User not found: " + userId);
        return new TenantUserAdminResponse(
            d.id(),
            d.keycloakSub() == null ? null : d.keycloakSub().value().toString(),
            d.username(), d.email(), d.firstName(), d.lastName(), d.displayName());
    }

    private static Optional<Locale> parseLocale(String locale) {
        if (locale == null || locale.isBlank()) return Optional.empty();
        try { return Optional.of(Locale.forLanguageTag(locale)); }
        catch (Exception e) { throw ProblemRest.badRequest("Invalid locale: " + locale); }
    }

    private static Optional<ZoneId> parseZone(String tz) {
        if (tz == null || tz.isBlank()) return Optional.empty();
        try { return Optional.of(ZoneId.of(tz)); }
        catch (Exception e) { throw ProblemRest.badRequest("Invalid timeZone: " + tz); }
    }

    private static Optional<Currency> parseCurrency(String cur) {
        if (cur == null || cur.isBlank()) return Optional.empty();
        try { return Optional.of(Currency.getInstance(cur.toUpperCase())); }
        catch (Exception e) { throw ProblemRest.badRequest("Invalid currency: " + cur); }
    }
}
