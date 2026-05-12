package com.tchalanet.server.platform.idempotence.internal.service;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.types.enums.IdempotencyScope;
import com.tchalanet.server.platform.idempotence.api.IdempotencyStore;
import com.tchalanet.server.platform.idempotence.internal.persistence.IdempotencyRecordJpaEntity;
import com.tchalanet.server.platform.idempotence.internal.persistence.IdempotencyRecordRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaIdempotencyStore implements IdempotencyStore {

  private final IdempotencyRecordRepository repo;

  @Override
  @Transactional
  public BeginResult begin(IdempotencyScope scope, String key, String requestHash, long ttlSeconds) {
    key = normalizeKey(key);
    var tenantId = requireTenantId();

    var e = new IdempotencyRecordJpaEntity();
    e.setId(UUID.randomUUID());
    e.setTenantId(tenantId);
    e.setScope(scope);
    e.setKey(key);
    e.setRequestHash(requestHash);
    e.setStatus(IdempotencyRecordJpaEntity.Status.IN_PROGRESS);
    e.setExpiresAt(Instant.now().plusSeconds(Math.max(1, ttlSeconds)));
    e.setCreatedBy(currentUserIdOrNull());

    try {
      repo.saveAndFlush(e);
      return new BeginResult(Decision.STARTED, Optional.empty());
    } catch (DataIntegrityViolationException dup) {
      var existing = repo.findByTenantIdAndScopeAndKey(tenantId, scope, key).orElse(null);
      if (existing == null) {
        return new BeginResult(Decision.IN_PROGRESS, Optional.empty());
      }

      if (!StringUtils.equals(existing.getRequestHash(), requestHash)) {
        return new BeginResult(Decision.PAYLOAD_MISMATCH, Optional.empty());
      }

      if (existing.getStatus() == IdempotencyRecordJpaEntity.Status.COMPLETED
          && existing.getResourceId() != null) {
        return new BeginResult(
            Decision.ALREADY_COMPLETED,
            Optional.of(new CompletedRecord(existing.getResourceId(), existing.getResponseJson())));
      }

      return new BeginResult(Decision.IN_PROGRESS, Optional.empty());
    }
  }

  @Override
  @Transactional
  public void complete(
      IdempotencyScope scope, String key, String requestHash, UUID resourceId, String responseJson) {

    key = normalizeKey(key);
    var tenantId = requireTenantId();

    repo.markCompleted(
        tenantId,
        scope,
        key,
        requestHash,
        IdempotencyRecordJpaEntity.Status.COMPLETED,
        resourceId,
        responseJson);
  }

  @Override
  @Transactional
  public void fail(IdempotencyScope scope, String key, String requestHash) {
    // MVP: leave IN_PROGRESS to expire naturally
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CompletedRecord> findCompleted(IdempotencyScope scope, String key) {
    key = normalizeKey(key);
    var tenantId = requireTenantId();

    return repo.findByTenantIdAndScopeAndKey(tenantId, scope, key)
        .filter(e -> e.getStatus() == IdempotencyRecordJpaEntity.Status.COMPLETED)
        .filter(e -> e.getResourceId() != null)
        .map(e -> new CompletedRecord(e.getResourceId(), e.getResponseJson()));
  }

  private String normalizeKey(String key) {
    return key == null ? null : key.trim();
  }

  private UUID requireTenantId() {
    var ctx = TchContext.currentOrNull();
    if (ctx == null || ctx.tenantUuid() == null) {
      throw new IllegalStateException("Tenant required for idempotency");
    }
    return ctx.tenantUuid();
  }

  private UUID currentUserIdOrNull() {
    var ctx = TchContext.currentOrNull();
    return (ctx != null) ? ctx.userUuid() : null;
  }
}
