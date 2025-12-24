package com.tchalanet.server.core.external.port.out;

import java.util.Map;
import java.util.UUID;

public interface KeycloakUserProvisioningPort {
    Map<String, Object> createUser(Map<String, Object> payload);

    void resetPassword(String userId, String newPassword);

    void updateUserProfile(UUID keycloakId, String firstName, String lastName, String email, String locale);

    void disableUser(UUID keycloakId, String reason);
}
