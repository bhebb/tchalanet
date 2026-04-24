package com.tchalanet.server.core.autonomy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyRule;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyMeta;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@UseCase
@Component
@RequiredArgsConstructor
public class GetAutonomyOverviewQueryHandler
    implements QueryHandler<GetAutonomyOverviewQuery, AutonomyOverviewView> {

  private final AutonomyPolicyRuleRepositoryPort repository;
  private final TchContextResolver tchContextResolver;

  @Override
  public AutonomyOverviewView handle(GetAutonomyOverviewQuery query) {
    if (query.targetType() == null) {
      throw new IllegalArgumentException("targetType is required");
    }

    // Derive tenant UUID when target is TENANT and no targetId was provided
    UUID effectiveTargetId = query.targetId();
    if (query.targetType() == AutonomyTargetType.TENANT && effectiveTargetId == null) {
      effectiveTargetId = tchContextResolver.currentOrThrow().tenantUuid();
    }

    // Read overview for the exact requested scope (do NOT perform hierarchy fallbacks)
    Optional<AutonomyPolicyRule> found = repository.findByTarget(query.targetType(), effectiveTargetId);

    AutonomyTargetId wrappedTargetId = effectiveTargetId == null ? null : AutonomyTargetId.of(effectiveTargetId);

    if (found.isPresent()) {
      AutonomyPolicyRule p = found.get();
      AutonomyRule rule =
          new AutonomyRule(
              p.getLevel(),
              p.isRequireApprovalOnBlock(),
              p.getApprovalRole(),
              p.isEnabled(),
              p.getStartsAt() == null ? null : p.getStartsAt().toInstant(),
              p.getEndsAt() == null ? null : p.getEndsAt().toInstant());

      AutonomyMeta meta =
          new AutonomyMeta(
              true,
              p.isDeleted(),
              p.getId() == null ? null : p.getId().value(),
              p.getVersion() == null ? 0L : p.getVersion());

      return new AutonomyOverviewView(query.targetType(), wrappedTargetId, rule, meta);
    }

    // No configured rule for this exact scope
    AutonomyRule emptyRule = null;
    AutonomyMeta meta = new AutonomyMeta(false, false, null, 0L);
    return new AutonomyOverviewView(query.targetType(), wrappedTargetId, emptyRule, meta);
  }
}
