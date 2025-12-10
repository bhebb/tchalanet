package com.tchalanet.server.core.limitpolicy.application;

import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.domain.ports.in.EvaluateLimitsForTicketUseCase;
import com.tchalanet.server.core.limitpolicy.domain.ports.out.GameReadModelPort;
import com.tchalanet.server.core.limitpolicy.domain.ports.out.LimitPolicyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CheckDailyLimitWinPerAgentCommandHandler implements EvaluateLimitsForTicketUseCase {

  private final LimitPolicyRepositoryPort limitPolicyRepository;
  private final GameReadModelPort gameReadModel; // To validate game codes

  @Override
  public LimitEvaluationResult evaluate(LimitEvaluationCommand command) {
    List<String> reasons = new ArrayList<>();
    BreachOutcome highestBreachOutcome = BreachOutcome.ALLOW;

    // 1. Fetch all active limit policies for the tenant
    List<LimitPolicy> policies = limitPolicyRepository.findActivePolicies(command.tenantId());

    // Sort policies to apply BLOCK first, then WARN, then ALLOW
    policies.sort(Comparator.comparing(LimitPolicy::getOnBreach));

    for (LimitPolicy policy : policies) {
      // Evaluate each line against relevant policies
      for (TicketLineInfo line : command.lines()) {
        // Check if policy applies to this line/context
        if (appliesTo(policy, command, line)) {
          BreachOutcome lineOutcome = evaluateLineAgainstPolicy(policy, line);
          if (lineOutcome.compareTo(highestBreachOutcome) > 0) {
            highestBreachOutcome = lineOutcome;
          }
          if (lineOutcome != BreachOutcome.ALLOW) {
            reasons.add(
                String.format(
                    "Limit breached for game %s (policy %s, scope %s, target %s): %s",
                    line.gameCode(),
                    policy.getId(),
                    policy.getScope(),
                    policy.getTarget(),
                    lineOutcome.name()));
          }
        }
      }
    }

    return new LimitEvaluationResult(highestBreachOutcome, reasons);
  }

  private boolean appliesTo(
      LimitPolicy policy, LimitEvaluationCommand command, TicketLineInfo line) {
    return switch (policy.getScope()) {
      case GLOBAL -> true;
      case GAME -> policy.getTarget().equals(line.gameCode());
      case TERMINAL -> policy.getTarget().equals(command.terminalId().toString());
      case USER -> policy.getTarget().equals(command.userId().toString());
      case SESSION -> policy.getTarget().equals(command.sessionId().toString());
      case SELECTION -> policy.getTarget().equals(line.selection());
      default -> false;
    };
  }

  private BreachOutcome evaluateLineAgainstPolicy(LimitPolicy policy, TicketLineInfo line) {
    BreachOutcome currentOutcome = BreachOutcome.ALLOW;

    // Check max stake per line
    if (policy.getMaxStakePerLine() != null
        && line.stake().compareTo(policy.getMaxStakePerLine()) > 0) {
      currentOutcome = policy.getOnBreach();
    }
    // Check max payout per line (assuming potential payout is calculated elsewhere or passed)
    // if (policy.getMaxPayoutPerLine() != null &&
    // line.potentialPayout().compareTo(policy.getMaxPayoutPerLine()) > 0) {
    //     if (policy.getOnBreach().compareTo(currentOutcome) > 0) currentOutcome =
    // policy.getOnBreach();
    // }
    // Daily cap would require aggregating previous stakes/payouts for the day,
    // which would involve another read model port or a dedicated service.

    return currentOutcome;
  }
}
