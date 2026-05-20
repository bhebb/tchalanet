package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Service interface for generating new UUIDs.
 *
 * <p>Used by domain factories to create new entity identifiers.
 * Allows mocking/stubbing in tests.
 */
public interface IdGenerator {
  UUID newUuid();
}
