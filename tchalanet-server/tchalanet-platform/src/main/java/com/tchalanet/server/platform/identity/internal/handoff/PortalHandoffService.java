package com.tchalanet.server.platform.identity.internal.handoff;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.ConsumePortalHandoffResponse;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.CreatePortalHandoffResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PortalHandoffService {
  private static final int SECRET_BYTES = 32;
  private static final Set<String> ADMIN_ROLES = Set.of("TENANT_ADMIN", "TENANT_OWNER");

  private final PortalHandoffJpaRepository handoffs;
  private final PortalAuthHandoffProperties properties;
  private final TchContextResolver contextResolver;
  private final ProviderSessionTokenIssuer tokenIssuer;
  private final PortalHandoffAudit audit;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock = Clock.systemUTC();

  @Transactional
  CreatePortalHandoffResponse create(
      PortalTarget targetPortal, String entryRoute, UUID supportAccessSessionId) {
    var ctx = contextResolver.currentOrThrow();
    var subjectUserId = ctx.userUuid();
    if (subjectUserId == null) {
      throw ProblemRest.unauthorized("portal_handoff.authenticated_user_required");
    }
    validateTargetAccess(ctx.roleCodes(), targetPortal, supportAccessSessionId);
    validateEntryRoute(entryRoute);

    var code = generateSecret();
    var now = Instant.now(clock);
    var entity = new PortalHandoffJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setCodeHash(sha256Hex(code));
    entity.setSubjectUserId(subjectUserId);
    entity.setTargetPortal(targetPortal);
    entity.setEntryRoute(entryRoute.trim());
    entity.setSupportAccessSessionId(supportAccessSessionId);
    entity.setCreatedAt(now);
    entity.setExpiresAt(now.plus(properties.ttl()));
    handoffs.save(entity);

    audit.log("PORTAL_HANDOFF_CREATED", entity.getId(), subjectUserId, targetPortal);
    return new CreatePortalHandoffResponse(
        entity.getId(),
        code,
        targetPortal,
        properties.targetUrl(targetPortal),
        entity.getEntryRoute(),
        entity.getExpiresAt());
  }

  @Transactional
  ConsumePortalHandoffResponse consume(UUID handoffId, String code, PortalTarget expectedTarget) {
    var handoff =
        handoffs.findById(handoffId).orElseThrow(() -> gone("portal_handoff.not_found_or_expired"));
    var now = Instant.now(clock);

    if (handoff.getTargetPortal() != expectedTarget) {
      audit.log(
          "PORTAL_HANDOFF_TARGET_MISMATCH", handoffId, handoff.getSubjectUserId(), expectedTarget);
      throw ProblemRest.forbidden("portal_handoff.target_mismatch");
    }
    if (handoff.getConsumedAt() != null) {
      audit.log(
          "PORTAL_HANDOFF_REPLAY_DETECTED", handoffId, handoff.getSubjectUserId(), expectedTarget);
      throw gone("portal_handoff.replayed");
    }
    if (!now.isBefore(handoff.getExpiresAt())) {
      audit.log("PORTAL_HANDOFF_EXPIRED", handoffId, handoff.getSubjectUserId(), expectedTarget);
      throw gone("portal_handoff.expired");
    }
    if (!constantTimeEquals(handoff.getCodeHash(), sha256Hex(code))) {
      throw ProblemRest.forbidden("portal_handoff.invalid_code");
    }

    var updated = handoffs.markConsumed(handoffId, now);
    if (updated != 1) {
      audit.log(
          "PORTAL_HANDOFF_REPLAY_DETECTED", handoffId, handoff.getSubjectUserId(), expectedTarget);
      throw gone("portal_handoff.replayed");
    }

    var customToken = tokenIssuer.createCustomToken(handoff.getSubjectUserId());
    audit.log("PORTAL_HANDOFF_CONSUMED", handoffId, handoff.getSubjectUserId(), expectedTarget);
    return new ConsumePortalHandoffResponse(
        customToken,
        handoff.getTargetPortal(),
        handoff.getEntryRoute(),
        handoff.getSupportAccessSessionId());
  }

  private void validateTargetAccess(
      Set<String> roles, PortalTarget targetPortal, UUID supportAccessSessionId) {
    var normalizedRoles = roles == null ? Set.<String>of() : roles;
    if (targetPortal == PortalTarget.PLATFORM && normalizedRoles.contains("SUPER_ADMIN")) {
      return;
    }
    if (targetPortal == PortalTarget.ADMIN
        && (normalizedRoles.stream().anyMatch(ADMIN_ROLES::contains)
            || (normalizedRoles.contains("SUPER_ADMIN") && supportAccessSessionId != null))) {
      return;
    }
    throw ProblemRest.forbidden("portal_handoff.target_forbidden");
  }

  private void validateEntryRoute(String entryRoute) {
    var normalized = entryRoute == null ? "" : entryRoute.trim();
    if (!properties.isAllowedEntryRoute(normalized)) {
      throw ProblemRest.badRequest("portal_handoff.entry_route_not_allowed");
    }
  }

  private String generateSecret() {
    var bytes = new byte[SECRET_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256Hex(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  private static boolean constantTimeEquals(String leftHex, String rightHex) {
    var left = HexFormat.of().parseHex(leftHex);
    var right = HexFormat.of().parseHex(rightHex);
    return MessageDigest.isEqual(left, right);
  }

  private static RuntimeException gone(String detail) {
    return ProblemRest.of(HttpStatus.GONE, detail);
  }
}
