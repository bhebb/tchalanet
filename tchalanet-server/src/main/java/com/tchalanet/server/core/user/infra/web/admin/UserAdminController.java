package com.tchalanet.server.core.user.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.user.application.command.model.BlockUserCommand;
import com.tchalanet.server.core.user.application.command.model.CreateUserCommand;
import com.tchalanet.server.core.user.application.command.model.DeactivateUserCommand;
import com.tchalanet.server.core.user.application.command.model.SoftDeleteUserCommand;
import com.tchalanet.server.core.user.application.command.model.UnblockUserCommand;
import com.tchalanet.server.core.user.application.query.model.GetUserDetailsQuery;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.infra.web.dto.CreateUserRequest;
import com.tchalanet.server.core.user.infra.web.dto.DeactivateUserRequest;
import com.tchalanet.server.core.user.infra.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin-api/users")
@RequiredArgsConstructor
public class UserAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  public ResponseEntity<UUID> createUser(@RequestBody CreateUserRequest req) {
    var command = new CreateUserCommand(
        req.tenantIdInitiator(),
        req.email(),
        req.phone(),
        req.firstName(),
        req.lastName(),
        req.locale(),
        req.sendInvitation(),
        req.initialRoles()
    );
    AppUser saved = commandBus.send(command);
    return ResponseEntity.ok(saved.id());
  }

  @PutMapping
  public ResponseEntity<UserResponse> updateUser(@RequestBody com.tchalanet.server.core.user.infra.web.dto.ReplaceUserRequest req) {
    var command = new com.tchalanet.server.core.user.application.command.model.ReplaceUserCommand(
        req.id(),
        req.username(),
        req.email(),
        req.phone(),
        req.firstName(),
        req.lastName(),
        req.displayName(),
        req.avatarUrl(),
        req.status(),
        req.locale(),
        req.timeZone()
    );

    var updated = commandBus.send(command);

    var res = new UserResponse(updated.id(), updated.keycloakId(), updated.tenantId(), updated.username(), updated.email(), updated.firstName(), updated.lastName(), updated.displayName());
    return ResponseEntity.ok(res);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    commandBus.send(new SoftDeleteUserCommand(id));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/block")
  public ResponseEntity<Void> blockUser(@PathVariable UUID id, @RequestBody(required = false) DeactivateUserRequest req) {
    var reason = Optional.ofNullable(req).map(DeactivateUserRequest::reason).orElse("blocked_by_admin");
    var details = queryBus.send(new GetUserDetailsQuery(id));
    UUID keycloakId = details == null ? null : details.keycloakId;

    commandBus.send(new BlockUserCommand(id, keycloakId, reason));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/unblock")
  public ResponseEntity<Void> unblockUser(@PathVariable UUID id) {
    commandBus.send(new UnblockUserCommand(id));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateUser(@PathVariable UUID id, @RequestHeader("X-Performed-By") UUID performedBy, @RequestBody(required = false) DeactivateUserRequest request) {
    var cmd = new DeactivateUserCommand(id, performedBy, Optional.ofNullable(request).map(DeactivateUserRequest::reason));
    commandBus.send(cmd);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserDetails(@PathVariable UUID id) {
    var details = queryBus.send(new GetUserDetailsQuery(id));
    if (details == null) return ResponseEntity.notFound().build();
    var res = new UserResponse(details.id, details.keycloakId, details.tenantId, details.username, details.email, details.firstName, details.lastName, details.displayName);

    return ResponseEntity.ok(res);
  }

  @GetMapping("/tenant/{tenantId}/active")
  public ResponseEntity<Page<UserResponse>> listActiveUsersByTenant(
      @PathVariable UUID tenantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    var pageResult = queryBus.send(new com.tchalanet.server.core.user.application.query.model.PagedListTenantUsersQuery(tenantId, page, size));
    var content = pageResult.getContent().stream().map(u -> new UserResponse(u.id(), u.keycloakId(), u.tenantId(), u.username(), u.email(), u.firstName(), u.lastName(), u.displayName())).collect(Collectors.toList());
    Page<UserResponse> result = new PageImpl<>(content, pageResult.getPageable(), pageResult.getTotalElements());
    return ResponseEntity.ok(result);
  }

  @GetMapping
  public ResponseEntity<Page<UserResponse>> listAllUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    var pageResult = queryBus.send(new com.tchalanet.server.core.user.application.query.model.PagedListAllUsersQuery(page, size));
    var content = pageResult.getContent().stream().map(u -> new UserResponse(u.id(), u.keycloakId(), u.tenantId(), u.username(), u.email(), u.firstName(), u.lastName(), u.displayName())).collect(Collectors.toList());
    Page<UserResponse> result = new PageImpl<>(content, pageResult.getPageable(), pageResult.getTotalElements());
    return ResponseEntity.ok(result);
  }
}
