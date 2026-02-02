package com.tchalanet.server.features.tenantadmin.users;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.tenantadmin.users.model.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@RestController
@RequestMapping("/admin}/users")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminUsersController {

    private final TenantAdminUsersOrchestrator orchestrator;
    private final TenantUserWebMapper mapper;

    @GetMapping("/bootstrap")
    public ApiResponse<TenantUserDetails> getOverview(@CurrentContext TchRequestContext ctx) {
        TenantUserDetails res = orchestrator.getBootstrap(ctx);
        return ApiResponse.success(res);
    }

    @GetMapping
    public ApiResponse<TchPage<TenantUserRow>> list(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "status", required = false) TenantUserStatus status,
        @RequestParam(value = "createdAfter", required = false) String createdAfter,
        @TchPaging(
            allowedSort = {"username", "displayName", "email", "createdAt", "status", "role", "outletId", "terminalId"},
            defaultSort = {"createdAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        Optional<String> qOpt = Optional.ofNullable(q).filter(s -> !s.isBlank());
        Optional<TenantUserStatus> statusOpt = Optional.ofNullable(status);

        Optional<Instant> createdAfterOpt = Optional.empty();
        if (createdAfter != null && !createdAfter.isBlank()) {
            try {
                createdAfterOpt = Optional.of(Instant.parse(createdAfter));
            } catch (DateTimeParseException ex) {
                throw ProblemRest.badRequest("Invalid createdAfter, expected ISO-8601 instant: " + createdAfter);
            }
        }

        var filter = new TenantUserFilter(qOpt, statusOpt, createdAfterOpt);

        var page =
            orchestrator.listTenantUsers(ctx, filter, pageReq);

        TchPage<TenantUserRow> res = TchPageMapper.map(page, mapper::toRow);
        return ApiResponse.success(res);
    }

    @GetMapping("/{userId}/overview")
    public ApiResponse<TenantUserResponse> getUserOverview(@CurrentContext TchRequestContext ctx, @PathVariable UserId userId) {
        TenantUserDetails details = orchestrator.getUserScreen(ctx, userId);
        var resp = mapper.toResponse(details);
        return ApiResponse.success(resp);
    }


    @GetMapping("/{userId}")
    public ApiResponse<TenantUserResponse> getDetails(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId
    ) {
        var details = orchestrator.getUserScreen(ctx, userId);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @PostMapping
    public ApiResponse<TenantUserResponse> create(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CreateUserRequest req
    ) {
        // Guardrail simple côté web (le core doit aussi valider)
        if (req.role() == null) throw ProblemRest.badRequest("role is required");
        if (req.role() == TchRole.CASHIER && req.outletId() == null) {
            throw ProblemRest.badRequest("outletId is required when role=CASHIER");
        }

        TenantUserDetails details = orchestrator.createUserAndAssign(ctx, req);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @PutMapping("/{userId}")
    public ApiResponse<TenantUserResponse> updateProfile(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdateUserRequest req
    ) {
        TenantUserDetails details = orchestrator.updateUser(ctx, userId, req);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @PatchMapping("/{userId}/preferences")
    public ApiResponse<TenantUserResponse> updatePreferences(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpdatePreferencesRequest req
    ) {
        var details = orchestrator.updatePreferences(ctx, userId, req);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @PutMapping("/{userId}/membership")
    public ApiResponse<TenantUserResponse> upsertMembership(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody UpsertMembershipRequest req
    ) {
        TenantUserDetails details = orchestrator.upsertMembership(ctx, userId, req);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @DeleteMapping("/{userId}/membership")
    public ApiResponse<TenantUserResponse> deleteMembership(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId
    ) {
        TenantUserDetails details = orchestrator.unassignUser(ctx, userId);
        return ApiResponse.success(mapper.toResponse(details));
    }

    @PutMapping("/{userId}/role")
    public ApiResponse<TenantUserResponse> setRole(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UserId userId,
        @Valid @RequestBody SetUserRoleRequest req
    ) {
        if (req.role() == null) {
            throw ProblemRest.badRequest("role is required");
        }
        TenantUserDetails details = orchestrator.setUserRole(ctx, userId, req.role());
        return ApiResponse.success(mapper.toResponse(details));
    }
}
