package com.tchalanet.server.features.tenantadmin.users;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.UnassignUserFromTenantCommand;
import com.tchalanet.server.core.tenantuser.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.user.application.command.model.*;
import com.tchalanet.server.core.user.application.query.model.CurrentUserDetails;
import com.tchalanet.server.core.user.application.query.model.GetCurrentUserQuery;
import com.tchalanet.server.features.tenantadmin.users.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantAdminUsersOrchestrator {

    private final CommandBus commandBus;
    private final QueryBus queryBus;


    public TenantUserDetails getBootstrap(TchRequestContext ctx) {
        if (ctx.userId() == null) {
            if (ctx.keycloakUserId() == null) throw ProblemRest.forbidden("Missing Keycloak subject (sub) in token");
            var cmd = new EnsureUserExistsForPrincipalCommand(
                com.tchalanet.server.common.types.id.KeycloakUserSub.parse(ctx.keycloakUserId()),
                ctx.effectiveTenantCode(),
                ctx.keycloakUserId(),
                null,
                null,
                null,
                null,
                null,
                ctx.locale(),
                ctx.tenantZoneId()
            );
            EnsureUserExistsForPrincipalResult ensureResult = commandBus.send(cmd);
            CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(ensureResult.userId()));
            if (details == null)
                throw ProblemRest.internal("Unable to load user after bootstrap: " + ensureResult.userId());
            return mapCurrentUserToTenantUserDetails(details);
        }
        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(ctx.userId()));
        if (details == null) throw ProblemRest.notFound("User not found: " + ctx.userId());
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails getMe(TchRequestContext ctx) {
        if (ctx.userId() == null) throw ProblemRest.notFound("User not found for current principal");
        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(ctx.userId()));
        if (details == null) throw ProblemRest.notFound("User not found: " + ctx.userId());
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TchPage<com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow> listTenantUsers(
        TchRequestContext ctx,
        TenantUserFilter filter,
        TchPageRequest pageReq
    ) {
        var q = new PagedListTenantUsersQuery(ctx.tenantId(), pageReq);
        var page = queryBus.send(q);
        return TchPageMapper.map(page, r -> r);
    }

    public TenantUserDetails getUserScreen(TchRequestContext ctx, UserId userId) {
        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.notFound("User not found: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails createUserAndAssign(TchRequestContext ctx, CreateUserRequest req) {
        var createCmd = new CreateUserCommand(
            req.email(),
            req.phone(),
            req.firstName(),
            req.lastName(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false,
            java.util.Set.of()
        );
        CreateUserResult createRes = commandBus.send(createCmd);

        var assignCmd = new AssignUserToTenantCommand(
            ctx.tenantId(),
            createRes.userId(),
            null,
            req.outletId(),
            req.terminalId(),
            false
        );
        commandBus.send(assignCmd);

        commandBus.send(new com.tchalanet.server.core.accesscontrol.application.command.model.SetTenantUserRoleCommand(
            ctx.tenantId(),
            createRes.userId(),
            req.role()
        ));

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(createRes.userId()));
        if (details == null) throw ProblemRest.internal("Unable to load user after create: " + createRes.userId());
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails updateUser(TchRequestContext ctx, UserId userId, UpdateUserRequest req) {
        var cmd = new UpdateUserProfileCommand(
            userId,
            Optional.ofNullable(req.firstName()),
            Optional.ofNullable(req.lastName()),
            Optional.ofNullable(req.email()),
            Optional.ofNullable(req.phone()),
            Optional.empty() // [FIX] Added missing locale optional
        );
        commandBus.send(cmd);

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.internal("Unable to load user after update: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails updatePreferences(TchRequestContext ctx, UserId userId, UpdatePreferencesRequest req) {
        Optional<Locale> localeOpt = parseLocale(req.locale());
        Optional<ZoneId> zoneOpt = parseZone(req.timeZone());
        Optional<Currency> currencyOpt = parseCurrency(req.currency());

        var cmd = new UpdateUserPreferencesCommand(
            userId,
            Optional.ofNullable(req.themeMode()),
            Optional.ofNullable(req.density()),
            localeOpt,
            zoneOpt,
            currencyOpt
        );
        commandBus.send(cmd);

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.internal("Unable to load user after preferences update: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails upsertMembership(TchRequestContext ctx, UserId userId, UpsertMembershipRequest req) {
        var cmd = new AssignUserToTenantCommand(ctx.tenantId(), userId, null, req.outletId(), req.terminalId(), false);
        commandBus.send(cmd);

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.internal("Unable to load user after membership update: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails unassignUser(TchRequestContext ctx, UserId userId) {
        commandBus.send(new UnassignUserFromTenantCommand(ctx.tenantId(), userId));

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.internal("Unable to load user after unassign: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    public TenantUserDetails setUserRole(TchRequestContext ctx, UserId userId, TchRole role) {
        commandBus.send(new com.tchalanet.server.core.accesscontrol.application.command.model.SetTenantUserRoleCommand(
            ctx.tenantId(),
            userId,
            role
        ));

        CurrentUserDetails details = queryBus.send(new GetCurrentUserQuery(userId));
        if (details == null) throw ProblemRest.internal("Unable to load user after role change: " + userId);
        return mapCurrentUserToTenantUserDetails(details);
    }

    private static Optional<Locale> parseLocale(String locale) {
        if (locale == null || locale.isBlank()) return Optional.empty();
        try {
            return Optional.of(Locale.forLanguageTag(locale));
        } catch (Exception e) {
            throw ProblemRest.badRequest("Invalid locale: " + locale);
        }
    }

    private static Optional<ZoneId> parseZone(String tz) {
        if (tz == null || tz.isBlank()) return Optional.empty();
        try {
            return Optional.of(ZoneId.of(tz));
        } catch (Exception e) {
            throw ProblemRest.badRequest("Invalid timeZone: " + tz);
        }
    }

    private static Optional<Currency> parseCurrency(String cur) {
        if (cur == null || cur.isBlank()) return Optional.empty();
        try {
            return Optional.of(Currency.getInstance(cur.toUpperCase()));
        } catch (Exception e) {
            throw ProblemRest.badRequest("Invalid currency: " + cur);
        }
    }

    private static TenantUserDetails mapCurrentUserToTenantUserDetails(CurrentUserDetails d) {
        return new TenantUserDetails(
            d.id(),
            d.keycloakSub(),
            d.username(),
            d.email(),
            d.firstName(),
            d.lastName(),
            d.displayName()
        );
    }
}
