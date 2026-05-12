package com.tchalanet.server.platform.idempotence.api;

import com.tchalanet.server.common.types.enums.IdempotencyScope;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {

  BeginResult begin(IdempotencyScope scope, String key, String requestHash, long ttlSeconds);

  void complete(
      IdempotencyScope scope, String key, String requestHash, UUID resourceId, String responseJson);

  void fail(IdempotencyScope scope, String key, String requestHash);

  Optional<CompletedRecord> findCompleted(IdempotencyScope scope, String key);

  enum Decision {
    STARTED,
    IN_PROGRESS,
    PAYLOAD_MISMATCH,
    ALREADY_COMPLETED
  }

  record BeginResult(Decision decision, Optional<CompletedRecord> completed) {}

  record CompletedRecord(UUID resourceId, String responseJson) {}
}
