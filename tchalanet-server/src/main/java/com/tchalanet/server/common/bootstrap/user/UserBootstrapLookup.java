package com.tchalanet.server.common.bootstrap.user;

import java.util.Optional;
import java.util.UUID;

public interface UserBootstrapLookup {
  Optional<UUID> findAppUserIdByKeycloakSub(UUID keycloakSub);
}
