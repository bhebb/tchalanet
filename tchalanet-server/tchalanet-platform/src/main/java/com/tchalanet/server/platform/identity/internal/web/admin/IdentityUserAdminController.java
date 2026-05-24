package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.catalog.plan.api.PlanLimitKeys; // Import LimitKeys
import com.tchalanet.server.platform.entitlement.api.RequiredQuota;
import com.tchalanet.server.platform.entitlement.api.UsageKeys; // Import UsageKeys
import com.tchalanet.server.platform.identity.api.model.request.UpdateUserProfileRequest;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.service.UserAdminService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.CreateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.InvitationStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.KeycloakSyncStatus;
import com.tchalanet.server.platform.identity.internal.web.admin.model.SetUserRoleRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.TenantUserAdminResponse;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpdatePreferencesRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpdateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.UpsertMembershipRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/admin/identity/users")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin • Identity Users")
public class IdentityUserAdminController {

    private final CurrentUserProfileService profiles;
    private final UserAdminService users;
    private final TenantMembershipService memberships;

    @GetMapping
    @Operation(summary = "List identity users for current tenant")
    public ApiResponse<TchPage<TenantUserAdminResponse>> list(
        @CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
        var page = memberships.list(ctx.tenantId(), pageReq);
        return ApiResponse.success(TchPageMapper.map(page, row -> loadAndMap(ctx, row.id(), InvitationStatus.NOT_SENT, row.createdAt())));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get identity user details")
    public ApiResponse<TenantUserAdminResponse> getUser(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping
    @Operation(summary = "Create identity user")
    @RequiredQuota(
        limit = PlanLimitKeys.USERS_MAX,
        usage = UsageKeys.USERS_ACTIVE
    )
    @AuditLog(action = AuditAction.USER_CREATE, entity = AuditEntityType.USER, idExpression = "#result.data().id()")
    public ApiResponse<TenantUserAdminResponse> create(
        @CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateUserRequest req) {
        forbidSuperAdminAssignmentForTenantAdmin(ctx, req.role());
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
        return ApiResponse.success(loadAndMap(ctx, created.userId(), InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> approve(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        users.approveUser(userId, ctx.currentUserIdRequired());
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update identity user profile")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> updateProfile(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdateUserRequest req) {
        assertTenantScopedUser(ctx, userId);
        profiles.updateProfile(
            new UpdateUserProfileRequest(
                userId,
                Optional.ofNullable(req.firstName()),
                Optional.ofNullable(req.lastName()),
                Optional.empty(),
                Optional.ofNullable(req.phone()),
                Optional.empty()));
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PatchMapping("/{userId}/preferences")
    @Operation(summary = "Update identity user preferences")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER_PREFERENCE, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> updatePreferences(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdatePreferencesRequest req) {
        assertTenantScopedUser(ctx, userId);
        profiles.updatePreferences(
            userId,
            req.themeMode(),
            req.density(),
            parseLocale(req.locale()).orElse(null),
            parseZone(req.timeZone()).orElse(null),
            parseCurrency(req.currency()).orElse(null));
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}/membership")
    @Operation(summary = "Assign or update tenant membership")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> upsertMembership(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpsertMembershipRequest req) {
        assertTenantScopedUser(ctx, userId);
        memberships.assign(ctx.tenantId(), userId, null, req.outletId(), req.terminalId(), false);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @DeleteMapping("/{userId}/membership")
    @Operation(summary = "Remove tenant membership")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> deleteMembership(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        memberships.unassign(ctx.tenantId(), userId);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Set tenant role")
    @AuditLog(action = AuditAction.USER_ROLE_CHANGE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> setRole(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody SetUserRoleRequest req) {
        assertTenantScopedUser(ctx, userId);
        forbidSuperAdminAssignmentForTenantAdmin(ctx, req.role());
        memberships.setRole(ctx.tenantId(), userId, req.role());
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> suspend(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        users.suspendUser(userId);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/reactivate")
    @Operation(summary = "Reactivate identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> reactivate(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        users.reactivateUser(userId);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/send-invitation")
    @Operation(summary = "Send invitation to identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> sendInvitation(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        users.sendInvitation(userId);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.SENT, null));
    }

    @PostMapping("/{userId}/resync-keycloak")
    @Operation(summary = "Resynchronize user with Keycloak")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> resyncKeycloak(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        assertTenantScopedUser(ctx, userId);
        users.resyncKeycloak(userId);
        return ApiResponse.success(loadAndMap(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    private TenantUserAdminResponse loadAndMap(
        TchRequestContext ctx, UserId userId, InvitationStatus invitationStatus, Instant createdAtOverride) {
        assertTenantScopedUser(ctx, userId);
        var profile = profiles.getUserProfile(userId);
        var membership = memberships.findByTenantAndUser(ctx.tenantId(), userId).orElse(null);
        var createdAt = createdAtOverride != null ? createdAtOverride : memberships.findCreatedAt(ctx.tenantId(), userId).orElse(null);
        return new TenantUserAdminResponse(
            profile.id(),
            profile.keycloakSub() == null ? null : profile.keycloakSub().value().toString(),
            profile.username(),
            profile.email(),
            profile.phone(),
            profile.status() == null ? null : profile.status().name(),
            membership == null || membership.roleId() == null ? null : membership.roleId().value().toString(),
            membership == null || membership.status() == null ? null : membership.status().name(),
            membership == null ? null : membership.outletId(),
            membership == null ? null : membership.terminalId(),
            resolveSyncStatus(profile, membership),
            invitationStatus,
            createdAt,
            profile.firstName(),
            profile.lastName(),
            profile.displayName());
    }

    private static void forbidSuperAdminAssignmentForTenantAdmin(TchRequestContext ctx, TchRole role) {
        if (ctx.isTenantAdmin() && role == TchRole.SUPER_ADMIN) {
            throw ProblemRest.forbidden("Tenant admin cannot assign SUPER_ADMIN role");
        }
    }

    private void assertTenantScopedUser(TchRequestContext ctx, UserId userId) {
        if (memberships.findByTenantAndUser(ctx.tenantId(), userId).isEmpty()) {
            throw ProblemRest.forbidden("User is outside effective tenant scope");
        }
    }

    private static KeycloakSyncStatus resolveSyncStatus(
        com.tchalanet.server.platform.identity.api.model.view.UserProfileView profile,
        TenantMembership membership) {
        if (membership == null) {
            return KeycloakSyncStatus.NOT_REQUIRED;
        }
        return profile.keycloakSub() == null ? KeycloakSyncStatus.PENDING : KeycloakSyncStatus.SYNCED;
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
