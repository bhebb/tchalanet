package com.tchalanet.server.platform.identity.internal.web.admin;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.internal.service.UserAdminService;
import com.tchalanet.server.platform.identity.internal.service.UserRow;
import com.tchalanet.server.platform.identity.internal.web.model.CreateUserRequest;
import com.tchalanet.server.platform.identity.internal.web.model.UserItemResponse;
import com.tchalanet.server.platform.identity.internal.web.model.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users")
@PreAuthorize("hasAuthority('TENANT_ADMIN') or hasAuthority('SUPER_ADMIN')")
public class UserAdminController {

  private final UserAdminService users;

  @Operation(summary = "Create a user (admin)")
  @PostMapping
  public ApiResponse<CreateUserResult> createUser(@RequestBody CreateUserRequest req) {
    return ApiResponse.success(
        users.createUser(
            req.email(),
            req.phone(),
            req.firstName(),
            req.lastName(),
            req.prefThemeMode(),
            req.prefDensity(),
            req.prefLocale(),
            req.prefTimeZone(),
            req.prefCurrency(),
            req.sendInvitation(),
            req.initialRoles()));
  }

  @PostMapping("/{id}/approve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void approveUser(@PathVariable UserId id) {
    users.approveUser(id, id);
  }

  @PostMapping("/{id}/suspend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void suspendUser(@PathVariable UserId id) {
    users.suspendUser(id);
  }

  @PostMapping("/{id}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reactivateUser(@PathVariable UserId id) {
    users.reactivateUser(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable UserId id) {
    users.deleteUser(id);
  }

  @GetMapping("/{id}")
  public ApiResponse<UserResponse> getUserDetails(@PathVariable UserId id) {
    var details = users.profile(id);
    if (details == null) {
      throw ProblemRest.notFound("User not found: " + id);
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

  @GetMapping
  public ApiResponse<TchPage<UserItemResponse>> listAllUsers(@TchPaging TchPageRequest pageReq) {
    return ApiResponse.success(TchPageMapper.map(users.listAll(pageReq), this::toUserItemResponse));
  }

  @GetMapping("/tenant/{tenantId}")
  public ApiResponse<TchPage<UserItemResponse>> listUsersByTenant(
      @PathVariable TenantId tenantId, @TchPaging TchPageRequest pageReq) {
    return ApiResponse.success(
        TchPageMapper.map(users.listByTenant(tenantId, pageReq), this::toUserItemResponse));
  }

  @GetMapping("/search")
  public ApiResponse<TchPage<UserItemResponse>> searchUsers(
      @RequestParam(required = false, name = "q") String nameLike,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String createdAfter,
      @RequestParam(required = false) String createdBefore,
      @TchPaging(allowedSort = {"createdAt", "username", "email", "status", "displayName"}, defaultSort = {"createdAt,DESC"})
          TchPageRequest pageReq) {
    return ApiResponse.success(
        TchPageMapper.map(
            users.search(nameLike, status, parseInstant(createdAfter), parseInstant(createdBefore), pageReq),
            this::toUserItemResponse));
  }

  private UserItemResponse toUserItemResponse(UserRow row) {
    return new UserItemResponse(row.id(), row.username(), row.email(), row.displayName(), row.status());
  }

  private static java.time.Instant parseInstant(String value) {
    return value == null || value.isBlank() ? null : java.time.Instant.parse(value);
  }
}
