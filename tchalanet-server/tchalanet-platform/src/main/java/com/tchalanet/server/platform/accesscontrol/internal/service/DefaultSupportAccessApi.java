package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.api.SupportAccessApi;
import com.tchalanet.server.platform.accesscontrol.api.model.StartSupportAccessSessionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.SupportAccessSessionView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.SupportAccessSessionJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.SupportAccessSessionJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class DefaultSupportAccessApi implements SupportAccessApi {

  private final SupportAccessSessionJpaRepository sessions;
  private final SupportAccessAudit audit;
  private final Clock clock = Clock.systemUTC();

  @Override
  @Transactional
  public SupportAccessSessionView start(StartSupportAccessSessionRequest request) {
    if (request.superAdminUserId() == null || request.superAdminUserId().value() == null) {
      throw ProblemRest.unauthorized("support_access.super_admin_required");
    }
    if (request.tenantId() == null || request.tenantId().value() == null) {
      throw ProblemRest.badRequest("support_access.tenant_required");
    }
    if (request.reason() == null || request.reason().trim().length() < 10) {
      throw ProblemRest.badRequest("support_access.reason_required");
    }

    var now = Instant.now(clock);
    sessions.clearCurrent(request.superAdminUserId().value(), now);

    var entity = new SupportAccessSessionJpaEntity();
    entity.setSuperAdminUserId(request.superAdminUserId().value());
    entity.setTenantId(request.tenantId().value());
    entity.setTenantCode(request.tenantCode());
    entity.setTenantName(request.tenantName());
    entity.setMode(request.mode());
    entity.setReason(request.reason().trim());
    entity.setGrantedAt(now);
    entity.setExpiresAt(now.plus(30, ChronoUnit.MINUTES));

    var saved = sessions.save(entity);
    audit.log("SUPPORT_ACCESS_STARTED", saved);
    return toView(saved);
  }

  @Override
  @Transactional
  public Optional<SupportAccessSessionView> current(UserId superAdminUserId) {
    if (superAdminUserId == null || superAdminUserId.value() == null) {
      return Optional.empty();
    }
    return currentSession(superAdminUserId, true).map(this::toView);
  }

  @Override
  @Transactional
  public void stop(UserId superAdminUserId) {
    if (superAdminUserId == null || superAdminUserId.value() == null) {
      return;
    }
    var current = currentSession(superAdminUserId, false);
    sessions.clearCurrent(superAdminUserId.value(), Instant.now(clock));
    current.ifPresent(session -> audit.log("SUPPORT_ACCESS_STOPPED", session));
  }

  @Override
  @Transactional
  public Optional<TenantId> currentTenant(UserId superAdminUserId) {
    return currentSession(superAdminUserId, false)
        .map(session -> TenantId.of(session.getTenantId()));
  }

  private Optional<SupportAccessSessionJpaEntity> currentSession(
      UserId superAdminUserId, boolean auditRestored) {
    var now = Instant.now(clock);
    var active =
        sessions.findFirstBySuperAdminUserIdAndClearedAtIsNullAndExpiresAtAfterOrderByGrantedAtDesc(
            superAdminUserId.value(), now);
    if (auditRestored) {
      active.ifPresent(session -> audit.log("SUPPORT_ACCESS_RESTORED", session));
    }
    if (active.isPresent()) {
      return active;
    }

    sessions
        .findFirstBySuperAdminUserIdAndClearedAtIsNullOrderByGrantedAtDesc(superAdminUserId.value())
        .filter(session -> !now.isBefore(session.getExpiresAt()))
        .ifPresent(
            session -> {
              session.setClearedAt(now);
              audit.log("SUPPORT_ACCESS_EXPIRED", session);
            });
    return Optional.empty();
  }

  private SupportAccessSessionView toView(SupportAccessSessionJpaEntity entity) {
    return new SupportAccessSessionView(
        entity.getId(),
        entity.getTenantId(),
        entity.getTenantCode(),
        entity.getTenantName(),
        entity.getGrantedAt(),
        entity.getExpiresAt(),
        "SUPER_ADMIN",
        entity.getMode(),
        entity.getMode().name().equals("SUPPORT_READONLY"));
  }
}
