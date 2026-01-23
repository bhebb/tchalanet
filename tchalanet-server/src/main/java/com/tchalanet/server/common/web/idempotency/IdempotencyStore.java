package com.tchalanet.server.common.web.idempotency;

import com.tchalanet.server.common.types.enums.IdempotencyScope;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {
  BeginResult begin(IdempotencyScope scope, String key, String requestHash, long ttlSeconds);

  void complete(IdempotencyScope scope, String key, String requestHash, UUID resourceId, String responseJson);

  void fail(IdempotencyScope scope, String key, String requestHash);

  Optional<CompletedRecord> findCompleted(IdempotencyScope scope, String key);

  record BeginResult(Decision decision, Optional<CompletedRecord> completed) {}

  enum Decision { STARTED, ALREADY_COMPLETED, IN_PROGRESS, PAYLOAD_MISMATCH }

  record CompletedRecord(UUID resourceId, String responseJson) {}
}
