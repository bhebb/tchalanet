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
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.TenantMembershipService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserAdministrationService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserProvisioningService;
import com.tchalanet.server.platform.identity.internal.web.admin.model.CreateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.admin.model.InvitationStatus;
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

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/admin/identity/users")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin • Identity Users")
public class IdentityUserAdminController {

    private final CurrentUserProfileService profiles;
    private final TenantUserAdministrationService users;
    private final TenantMembershipService memberships;
    private final AccessControlApi accessControlApi;
    private final TenantUserProvisioningService provisioning;
    private final TenantUserAdminViewAssembler view;

    @GetMapping
    @Operation(summary = "List identity users for current tenant")
    public ApiResponse<TchPage<TenantUserAdminResponse>> list(
        @CurrentContext TchRequestContext ctx, @TchPaging TchPageRequest pageReq) {
        var page = memberships.list(ctx.tenantId(), pageReq);
        return ApiResponse.success(TchPageMapper.map(page, row -> view.load(ctx, row.id(), InvitationStatus.NOT_SENT, row.createdAt())));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get identity user details")
    public ApiResponse<TenantUserAdminResponse> getUser(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
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
        var created = provisioning.provisionTenantUser(
            ctx.tenantId(), ctx.currentUserIdRequired(),
            req.email(), req.phone(), req.firstName(), req.lastName(),
            req.outletId(), req.terminalId(), req.role());
        return ApiResponse.success(view.load(ctx, created.userId(), InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> approve(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        users.approveUser(userId, ctx.currentUserIdRequired());
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update identity user profile")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> updateProfile(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdateUserRequest req) {
        view.assertTenantScoped(ctx, userId);
        profiles.updateProfile(
            new UpdateUserProfileRequest(
                userId,
                Optional.ofNullable(req.firstName()),
                Optional.ofNullable(req.lastName()),
                Optional.empty(),
                Optional.ofNullable(req.phone()),
                Optional.empty()));
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PatchMapping("/{userId}/preferences")
    @Operation(summary = "Update identity user preferences")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER_PREFERENCE, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> updatePreferences(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdatePreferencesRequest req) {
        view.assertTenantScoped(ctx, userId);
        profiles.updatePreferences(
            userId,
            req.themeMode(),
            req.density(),
            parseLocale(req.locale()).orElse(null),
            parseZone(req.timeZone()).orElse(null),
            parseCurrency(req.currency()).orElse(null));
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}/membership")
    @Operation(summary = "Assign or update tenant membership")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> upsertMembership(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpsertMembershipRequest req) {
        view.assertTenantScoped(ctx, userId);
        memberships.assign(ctx.tenantId(), userId, req.outletId(), req.terminalId(), false);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @DeleteMapping("/{userId}/membership")
    @Operation(summary = "Remove tenant membership")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> deleteMembership(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        memberships.unassign(ctx.tenantId(), userId);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Set tenant role")
    @AuditLog(action = AuditAction.USER_ROLE_CHANGE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> setRole(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody SetUserRoleRequest req) {
        view.assertTenantScoped(ctx, userId);
        forbidSuperAdminAssignmentForTenantAdmin(ctx, req.role());
        accessControlApi.assignRoleToUser(new AssignRoleToUserRequest(ctx.tenantId(), userId, req.role().name(), ctx.currentUserIdRequired()));
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> suspend(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        users.suspendUser(userId);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/reactivate")
    @Operation(summary = "Reactivate identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> reactivate(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        users.reactivateUser(userId);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    @PostMapping("/{userId}/send-invitation")
    @Operation(summary = "Send invitation to identity user")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> sendInvitation(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        users.sendInvitation(userId);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.SENT, null));
    }

    @PostMapping("/{userId}/resync-keycloak")
    @Operation(summary = "Resynchronize user with Keycloak")
    @AuditLog(action = AuditAction.USER_UPDATE, entity = AuditEntityType.USER, idExpression = "#userId")
    public ApiResponse<TenantUserAdminResponse> resyncKeycloak(
        @CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        view.assertTenantScoped(ctx, userId);
        users.resyncKeycloak(userId);
        return ApiResponse.success(view.load(ctx, userId, InvitationStatus.NOT_SENT, null));
    }

    private static void forbidSuperAdminAssignmentForTenantAdmin(TchRequestContext ctx, TchRole role) {
        if (ctx.isTenantAdmin() && role == TchRole.SUPER_ADMIN) {
            throw ProblemRest.forbidden("Tenant admin cannot assign SUPER_ADMIN role");
        }
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
