package com.tchalanet.server.core.user.web;

import com.tchalanet.server.core.user.domain.usecase.AssignUserToTenantUseCase;
import com.tchalanet.server.core.user.domain.usecase.CreateUserUseCase;
import com.tchalanet.server.core.user.domain.usecase.GetUserUseCase;
import com.tchalanet.server.core.user.domain.usecase.ListAllUsersUseCase;
import com.tchalanet.server.core.user.domain.usecase.ListTenantUsersUseCase;
import com.tchalanet.server.core.user.domain.usecase.SoftDeleteUserUseCase;
import com.tchalanet.server.core.user.domain.usecase.UnassignUserFromTenantUseCase;
import com.tchalanet.server.core.user.domain.usecase.UpdateUserUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class UserAdminController {

  private final CreateUserUseCase createUser;
  private final UpdateUserUseCase updateUser;
  private final AssignUserToTenantUseCase assignUserToTenant;
  private final SoftDeleteUserUseCase softDeleteUser;
  private final ListTenantUsersUseCase listTenantUsers;
  private final ListAllUsersUseCase listAllUsers;
  private final GetUserUseCase getUser;
  private final UnassignUserFromTenantUseCase unassignUserFromTenant;
  private final UserWebMapper mapper;

  public UserAdminController(
      CreateUserUseCase createUser,
      UpdateUserUseCase updateUser,
      AssignUserToTenantUseCase assignUserToTenant,
      SoftDeleteUserUseCase softDeleteUser,
      ListTenantUsersUseCase listTenantUsers,
      ListAllUsersUseCase listAllUsers,
      GetUserUseCase getUser,
      UnassignUserFromTenantUseCase unassignUserFromTenant,
      UserWebMapper mapper) {
    this.createUser = createUser;
    this.updateUser = updateUser;
    this.assignUserToTenant = assignUserToTenant;
    this.softDeleteUser = softDeleteUser;
    this.listTenantUsers = listTenantUsers;
    this.listAllUsers = listAllUsers;
    this.getUser = getUser;
    this.unassignUserFromTenant = unassignUserFromTenant;
    this.mapper = mapper;
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
    var domain = mapper.toDomain(req);
    var created = createUser.create(domain);
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> update(
      @PathVariable("id") UUID id, @Valid @RequestBody UpdateUserRequest req) {
    var domain = mapper.updateToDomain(id, req);
    var updated = updateUser.update(domain);
    return ResponseEntity.ok(mapper.toResponse(updated));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @PostMapping("/assign")
  public ResponseEntity<?> assignToTenant(@Valid @RequestBody AssignUserRequest req) {
    assignUserToTenant.assign(req.tenantId(), req.userId(), req.role());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> softDelete(@PathVariable("id") UUID id) {
    softDeleteUser.softDelete(id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @GetMapping("/tenant/{tenantId}")
  public ResponseEntity<List<UserResponse>> listByTenant(@PathVariable("tenantId") UUID tenantId) {
    var users = listTenantUsers.listByTenant(tenantId);
    return ResponseEntity.ok(users.stream().map(mapper::toResponse).collect(Collectors.toList()));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @GetMapping
  public ResponseEntity<List<UserResponse>> listAll() {
    var users = listAllUsers.listAll();
    return ResponseEntity.ok(users.stream().map(mapper::toResponse).collect(Collectors.toList()));
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getById(@PathVariable("id") UUID id) {
    return getUser
        .getById(id)
        .map(mapper::toResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN_ENTERPRISE')")
  @DeleteMapping("/{userId}/tenant/{tenantId}")
  public ResponseEntity<?> unassign(@PathVariable UUID tenantId, @PathVariable UUID userId) {
    unassignUserFromTenant.unassign(tenantId, userId);
    return ResponseEntity.noContent().build();
  }

  // --- DTOs as records (Java 21) with validation
  public static final record CreateUserRequest(
      UUID id,
      UUID tenantId,
      @NotBlank @Size(min = 3, max = 100) String username,
      @NotBlank @Size(max = 255) String email,
      String displayName,
      String locale) {}

  public static final record UpdateUserRequest(
      UUID tenantId,
      @NotBlank @Size(min = 3, max = 100) String username,
      @NotBlank @Size(max = 255) String email,
      String displayName,
      String locale) {}

  public static final record AssignUserRequest(
      UUID tenantId, UUID userId, @NotBlank @Size(max = 32) String role) {}

  public static final record UserResponse(
      UUID id,
      UUID tenantId,
      String username,
      String email,
      String displayName,
      String locale,
      Instant lastLoginAt) {}
}
