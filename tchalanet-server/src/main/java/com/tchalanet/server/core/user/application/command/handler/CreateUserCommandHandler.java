package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import com.tchalanet.server.core.accesscontrol.infra.persistence.TenantUserJpaEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.TenantUserJpaRepository;
import com.tchalanet.server.core.external.ports.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.CreateUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, AppUser> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakProvisioningPort;
  private final TenantUserJpaRepository tenantUserRepository;
  private final AppRoleJpaRepository appRoleRepository;

  @Override
  @Transactional
  public AppUser handle(CreateUserCommand command) {
    userReaderPort
        .findByEmailOrPhone(command.email(), command.phone())
        .ifPresent(u -> {
          throw new IllegalStateException("User already exists with this email or phone");
        });

    UUID keycloakId = provisionInKeycloak(command);

    AppUser newUser =
        new AppUser(
            UUID.randomUUID(),
            keycloakId,
            command.tenantIdInitiator(),
            resolveUsername(command),
            command.email(),
            command.phone(),
            command.firstName(),
            command.lastName(),
            buildDisplayName(command),
            null,
            UserStatus.ACTIVE,
            command.locale(),
            null,
            Instant.now());

    AppUser saved = userWriterPort.save(newUser);
    linkToTenant(saved.id(), command);
    return saved;
  }

  private UUID provisionInKeycloak(CreateUserCommand command) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("email", command.email());
    payload.put("phoneNumber", command.phone());
    payload.put("firstName", command.firstName());
    payload.put("lastName", command.lastName());
    payload.put("locale", command.locale());
    payload.put("sendInvitation", command.sendInvitation());
    payload.put("initialRoles", command.initialRoles());

    var response = keycloakProvisioningPort.createUser(payload);
    Object idValue = response.get("id");
    if (idValue == null) {
      throw new IllegalStateException("Keycloak did not return an id");
    }
    return UUID.fromString(idValue.toString());
  }

  private void linkToTenant(UUID userId, CreateUserCommand command) {
    TenantUserJpaEntity tenantUser = new TenantUserJpaEntity();
    tenantUser.setTenantId(command.tenantIdInitiator());
    tenantUser.setUserId(userId);
    tenantUser.setRoleId(resolveRole(command));
    tenantUserRepository.save(tenantUser);
  }

  private UUID resolveRole(CreateUserCommand command) {
    return Optional.ofNullable(command.initialRoles())
        .flatMap(set -> set.stream().findFirst())
        .flatMap(code -> appRoleRepository.findTenantRole(command.tenantIdInitiator(), code).map(AppRoleEntity::getId))
        .orElseThrow(() -> new IllegalStateException("No role available for tenant user"));
  }

  private String resolveUsername(CreateUserCommand command) {
    if (command.email() != null && !command.email().isBlank()) {
      return command.email();
    }
    if (command.phone() != null && !command.phone().isBlank()) {
      return command.phone();
    }
    throw new IllegalArgumentException("Either email or phone must be provided");
  }

  private String buildDisplayName(CreateUserCommand command) {
    return (Optional.ofNullable(command.firstName()).orElse("") + " " + Optional.ofNullable(command.lastName()).orElse("")).trim();
  }
}
