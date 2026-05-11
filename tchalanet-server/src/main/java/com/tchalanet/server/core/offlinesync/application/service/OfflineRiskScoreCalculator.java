package com.tchalanet.server.core.offlinesync.application.service;

import com.tchalanet.server.core.offlinesync.domain.model.OfflineRiskFlag;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSubmissionStatus;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OfflineRiskScoreCalculator {

  public Set<OfflineRiskFlag> calculate(OfflineSaleSubmission submission) {
    var flags = EnumSet.noneOf(OfflineRiskFlag.class);
    if (submission.status() == OfflineSubmissionStatus.SALES_REVIEW_REQUIRED) {
      flags.add(OfflineRiskFlag.FINALIZED_SESSION);
    }
    return flags;
  }
}

