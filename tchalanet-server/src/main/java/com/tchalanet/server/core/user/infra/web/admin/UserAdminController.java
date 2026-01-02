package com.tchalanet.server.core.user.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.command.model.*;
import com.tchalanet.server.core.user.application.query.model.GetUserDetailsQuery;
import com.tchalanet.server.core.user.application.query.model.UserProfileQuery;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.infra.web.dto.CreateUserRequest;
import com.tchalanet.server.core.user.infra.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin • Users")
public class UserAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "Create a user (admin)")
  @PostMapping
  public ResponseEntity<UserId> createUser(@RequestBody CreateUserRequest req) {
    var command =
        new CreateUserCommand(
            req.tenantIdInitiator(),
            req.email(),
            req.phone(),
            req.firstName(),
            req.lastName(),
            req.locale(),
            req.sendInvitation(),
            req.initialRoles());
    AppUser saved = commandBus.send(command);
    return ResponseEntity.ok(saved.getId());
  }

  @Operation(summary = "Approve a pending user (admin)")
  @PostMapping("/{id}/approve")
  public ResponseEntity<Void> approveUser(@PathVariable UserId id) {
    var cmd = new ApproveUserCommand(id, null);
    commandBus.send(cmd);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Suspend a user (admin)")
  @PostMapping("/{id}/suspend")
  public ResponseEntity<Void> suspendUser(@PathVariable UserId id) {
    var cmd = new SuspendUserCommand(id, "suspended_by_admin");
    commandBus.send(cmd);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Reactivate a user (admin)")
  @PostMapping("/{id}/reactivate")
  public ResponseEntity<Void> reactivateUser(@PathVariable UserId id) {
    var cmd = new ReactivateUserCommand(id);
    commandBus.send(cmd);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Delete a user (admin)")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable UserId id) {
    commandBus.send(new DeleteUserCommand(id));
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get user details (admin)")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserDetails(@PathVariable UserId id) {
    UserProfileQuery details = queryBus.send(new GetUserDetailsQuery(id.uuid()));
    if (details == null) return ResponseEntity.notFound().build();
    var res =
        new UserResponse(
            UserId.of(details.id),
            details.keycloakId,
            details.username,
            details.email,
            details.firstName,
            details.lastName,
            details.displayName);

    return ResponseEntity.ok(res);
  }

  @Operation(summary = "List active users for a tenant (admin)")
  @GetMapping("/tenant/{tenantId}/active")
  public ResponseEntity<Page<UserResponse>> listActiveUsersByTenant(
      @PathVariable TenantId tenantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Page<AppUser> pageResult =
        queryBus.send(
            new com.tchalanet.server.core.user.application.query.model.PagedListTenantUsersQuery(
                tenantId, page, size));
    var content =
        pageResult.getContent().stream()
            .map(
                u ->
                    new UserResponse(
                        u.getId(),
                        u.getKeycloakId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getDisplayName()))
            .collect(Collectors.toList());
    Page<UserResponse> result =
        new PageImpl<>(content, pageResult.getPageable(), pageResult.getTotalElements());
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "List all users (admin)")
  @GetMapping
  public ResponseEntity<Page<UserResponse>> listAllUsers(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

    Page<AppUser> pageResult =
        queryBus.send(
            new com.tchalanet.server.core.user.application.query.model.PagedListAllUsersQuery(
                page, size));
    var content =
        pageResult.getContent().stream()
            .map(
                u ->
                    new UserResponse(
                        u.getId(),
                        u.getKeycloakId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getDisplayName()))
            .collect(Collectors.toList());
    Page<UserResponse> result =
        new PageImpl<>(content, pageResult.getPageable(), pageResult.getTotalElements());
    return ResponseEntity.ok(result);
  }
}
