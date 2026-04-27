package com.tchalanet.server.core.user.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.user.application.command.model.*;
import com.tchalanet.server.core.user.application.query.model.GetUserDetailsQuery;
import com.tchalanet.server.core.user.application.query.model.PagedListAllUsersQuery;
import com.tchalanet.server.core.user.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.user.application.query.model.UserRow;
import com.tchalanet.server.core.user.infra.web.model.CreateUserRequest;
import com.tchalanet.server.core.user.infra.web.model.UserItemResponse;
import com.tchalanet.server.core.user.infra.web.model.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin • Users")
@PreAuthorize("hasAuthority('TENANT_ADMIN') or hasAuthority('SUPER_ADMIN')")
public class UserAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    // -------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------

    @Operation(summary = "Create a user (admin)")
    @PostMapping
    public ApiResponse<CreateUserResult> createUser(@RequestBody CreateUserRequest req) {
        var cmd =
            new CreateUserCommand(
                req.email(),
                req.phone(),
                req.firstName(),
                req.lastName(),
                ofNullable(req.prefThemeMode()),
                ofNullable(req.prefDensity()),
                ofNullable(req.prefLocale()),
                ofNullable(req.prefTimeZone()),
                ofNullable(req.prefCurrency()),
                req.sendInvitation(),
                req.initialRoles());
        var result = commandBus.send(cmd);
        return ApiResponse.success(result);
    }

    // -------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------

    @Operation(summary = "Approve a pending user (admin)")
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveUser(@PathVariable UserId id) {
        commandBus.send(new ApproveUserCommand(id, null));
    }

    @Operation(summary = "Suspend a user (admin)")
    @PostMapping("/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendUser(@PathVariable UserId id) {
        commandBus.send(new SuspendUserCommand(id, "suspended_by_admin"));
    }

    @Operation(summary = "Reactivate a user (admin)")
    @PostMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateUser(@PathVariable UserId id) {
        commandBus.send(new ReactivateUserCommand(id));
    }

    @Operation(summary = "Delete a user (admin)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UserId id) {
        commandBus.send(new DeleteUserCommand(id));
    }

    // -------------------------------------------------------------------
    // Read: details
    // -------------------------------------------------------------------

    @Operation(summary = "Get user details (admin)")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserDetails(@PathVariable UserId id) {
        var details = queryBus.send(new GetUserDetailsQuery(id));
        if (details == null) {
            throw new UserNotFoundException(id);
        }
        return ApiResponse.success(new UserResponse(details.id(), details.keycloakSub(), details.username(), details.email(), details.firstName(), details.lastName(), details.displayName()));
    }

    // -------------------------------------------------------------------
    // Read: list (paged)
    // -------------------------------------------------------------------

    @Operation(summary = "List users (admin)")
    @GetMapping
    public ApiResponse<TchPage<UserItemResponse>> listAllUsers(@TchPaging TchPageRequest pageReq) {
        // Per pagination conventions handlers must return TchPage<UserRow>.
        TchPage<UserRow> page = queryBus.send(new PagedListAllUsersQuery(pageReq));
        return ApiResponse.success(TchPageMapper.map(page, this::toUserItemResponse));
    }

    @Operation(summary = "List users for a tenant (admin)")
    @GetMapping("/tenant/{tenantId}")
    public ApiResponse<TchPage<UserItemResponse>> listUsersByTenant(
        @PathVariable TenantId tenantId, @TchPaging TchPageRequest pageReq) {

        TchPage<UserRow> page = queryBus.send(new PagedListTenantUsersQuery(tenantId, pageReq));
        return ApiResponse.success(TchPageMapper.map(page, this::toUserItemResponse));
    }

    @Operation(summary = "Search users (admin) - by name, status, created range")
    @GetMapping("/search")
    public ApiResponse<TchPage<UserItemResponse>> searchUsers(
        @RequestParam(required = false, name = "q") String nameLike,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String createdAfter,
        @RequestParam(required = false) String createdBefore,
        @TchPaging(allowedSort = {"createdAt", "username", "email", "status", "displayName"}, defaultSort = {"createdAt,DESC"}) TchPageRequest pageReq) {

        java.time.Instant after = null;
        java.time.Instant before = null;
        try {
            if (createdAfter != null && !createdAfter.isBlank()) after = java.time.Instant.parse(createdAfter);
        } catch (Exception ex) {
            throw new IllegalArgumentException("createdAfter must be an ISO instant");
        }
        try {
            if (createdBefore != null && !createdBefore.isBlank()) before = java.time.Instant.parse(createdBefore);
        } catch (Exception ex) {
            throw new IllegalArgumentException("createdBefore must be an ISO instant");
        }

        int p = pageReq.pageable().getPageNumber();
        int s = pageReq.pageable().getPageSize();
        TchPage<UserRow> page = queryBus.send(new com.tchalanet.server.core.user.application.query.model.SearchUsersQuery(nameLike, status, after, before, p, s));
        return ApiResponse.success(TchPageMapper.map(page, this::toUserItemResponse));
    }

    // -------------------------------------------------------------------
    // Mapping helpers (web only)
    // -------------------------------------------------------------------

    private UserItemResponse toUserItemResponse(UserRow r) {
        return new UserItemResponse(r.id(), r.username(), r.email(), r.displayName(), r.status());
    }
}
