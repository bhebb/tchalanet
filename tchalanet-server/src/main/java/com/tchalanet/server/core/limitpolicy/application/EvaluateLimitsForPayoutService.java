package com.tchalanet.server.core.limitpolicy.application;

import com.tchalanet.server.core.limitpolicy.application.ports.in.EvaluateLimitsForPayoutUseCase;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.application.ports.out.LimitPolicyRepositoryPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EvaluateLimitsForPayoutService implements EvaluateLimitsForPayoutUseCase {

  private final LimitPolicyRepositoryPort limitPolicyRepository;

  @Override
  public LimitEvaluationResult evaluate(PayoutLimitEvaluationCommand command) {
    List<String> reasons = new ArrayList<>();
    BreachOutcome highest = BreachOutcome.ALLOW;

    List<LimitPolicy> policies = limitPolicyRepository.findActivePolicies(command.tenantId());
    policies.sort(Comparator.comparing(LimitPolicy::getOnBreach));

    for (LimitPolicy policy : policies) {
      // check daily cap (if set) against payout amount
      if (policy.getDailyCap() != null && command.amount().compareTo(policy.getDailyCap()) > 0) {
        BreachOutcome outcome = policy.getOnBreach();
        if (outcome.compareTo(highest) > 0) highest = outcome;
        reasons.add(String.format("Policy %s breached: amount %s > dailyCap %s", policy.getId(), command.amount(), policy.getDailyCap()));
      }
      // check max payout per line as a conservative proxy (if set)
      if (policy.getMaxPayoutPerLine() != null && command.amount().compareTo(policy.getMaxPayoutPerLine()) > 0) {
        BreachOutcome outcome = policy.getOnBreach();
        if (outcome.compareTo(highest) > 0) highest = outcome;
        reasons.add(String.format("Policy %s breached: amount %s > maxPayoutPerLine %s", policy.getId(), command.amount(), policy.getMaxPayoutPerLine()));
      }
    }

    return new LimitEvaluationResult(highest, reasons);
  }
}
