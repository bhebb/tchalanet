package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import com.tchalanet.server.core.accesscontrol.infra.persistence.TenantUserJpaRepository;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import com.tchalanet.server.core.user.infra.web.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class EnsureUserExistsForPrincipalCommandHandler implements CommandHandler<EnsureUserExistsForPrincipalCommand, MeResponse> {

    private final UserReaderPort userReaderPort;
    private final UserWriterPort userWriterPort;
    private final TenantUserJpaRepository tenantUserRepository;
    private final AppRoleJpaRepository appRoleRepository;

    @Override
    @Transactional
    public MeResponse handle(EnsureUserExistsForPrincipalCommand command) {
        UUID keycloakUuid = parseKeycloakUuid(command.keycloakId());

        Optional<AppUser> existing = userReaderPort.findByKeycloakId(keycloakUuid);
        boolean isNew;
        AppUser user;

        if (existing.isPresent()) {
            user = updateExistingUser(existing.get(), command);
            isNew = false;
        } else {
            user = createNewUser(command, keycloakUuid);
            isNew = true;

            if (user.tenantId() != null) {
                attachUserToTenantIfPossible(user.tenantId(), user);
            }
        }

        return toMeResponse(user, isNew);
    }

    // --- extracted helpers ---
    private UUID parseKeycloakUuid(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("keycloakId is required");
        }
        try {
            return UUID.fromString(keycloakId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("keycloakId must be a UUID", ex);
        }
    }

    private AppUser updateExistingUser(AppUser user, EnsureUserExistsForPrincipalCommand command) {
        String email = command.email();
        String firstName = command.firstName();
        String lastName = command.lastName();

        String username = user.username();
        if (email != null && !email.equals(user.email())) {
            username = email;
        }

        boolean nameChanged = (firstName != null && !firstName.equals(user.firstName()))
            || (lastName != null && !lastName.equals(user.lastName()));

        String display = nameChanged ? buildDisplayName(firstName, lastName) : user.displayName();

        AppUser updated = new AppUser(
            user.id(),
            user.keycloakId(),
            user.tenantId(),
            username,
            email != null ? email : user.email(),
            user.phone(),
            firstName != null ? firstName : user.firstName(),
            lastName != null ? lastName : user.lastName(),
            display,
            user.avatarUrl(),
            user.status(),
            user.locale(),
            user.timeZone(),
            Instant.now());

        return userWriterPort.save(updated);
    }

    private AppUser createNewUser(EnsureUserExistsForPrincipalCommand command, UUID keycloakUuid) {
        UUID tenantId = null;
        Set<UUID> tenantIds = command.tenantIdsFromToken();
        if (tenantIds != null && !tenantIds.isEmpty()) {
            tenantId = tenantIds.iterator().next();
        }

        AppUser newUser = new AppUser(
            UUID.randomUUID(),
            keycloakUuid,
            tenantId,
            command.email(),
            command.email(),
            null,
            command.firstName(),
            command.lastName(),
            buildDisplayName(command.firstName(), command.lastName()),
            null,
            UserStatus.ACTIVE,
            null,
            null,
            Instant.now());

        return userWriterPort.save(newUser);
    }

    private void attachUserToTenantIfPossible(UUID tenantId, AppUser user) {
        var roles = appRoleRepository.findAllForTenantOrGlobal(tenantId);
        if (roles != null && !roles.isEmpty()) {
            roles.stream().findFirst().ifPresent(role -> {
                var tenantUser = new com.tchalanet.server.core.accesscontrol.infra.persistence.TenantUserJpaEntity();
                tenantUser.setTenantId(tenantId);
                tenantUser.setUserId(user.id());
                tenantUser.setRoleId(role.getId());
                tenantUser.setAutonomyLevel("none");
                tenantUser.setOwner(false);
                tenantUserRepository.save(tenantUser);
            });
        }
    }

    private MeResponse toMeResponse(AppUser user, boolean isNew) {
        return new MeResponse(
            user.id(),
            user.keycloakId(),
            user.tenantId(),
            user.username(),
            user.email(),
            user.firstName(),
            user.lastName(),
            user.displayName(),
            isNew
        );
    }

    private static String buildDisplayName(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName;
        String l = lastName == null ? "" : lastName;
        return (f + " " + l).trim();
    }
}
