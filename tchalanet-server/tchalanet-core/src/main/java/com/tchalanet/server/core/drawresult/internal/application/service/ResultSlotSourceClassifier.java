package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultProviderCapabilityPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResultSlotSourceClassifier {

  private final ResultSlotSourceConfigResolver sourceConfigResolver;
  private final ExternalResultProviderCapabilityPort providerCapabilityPort;

  public ResultSlotSourceClassification classify(ResultSlotView slot) {
    if (slot == null || !slot.active()) {
      return ResultSlotSourceClassification.INACTIVE;
    }

    var sourceCfg = sourceConfigResolver.resolve(slot.sourceCfg());
    if (!sourceCfg.hasAnyActiveGame()) {
      return ResultSlotSourceClassification.MANUAL;
    }

    return providerCapabilityPort.supportsAutomaticFetch(slot.provider())
        ? ResultSlotSourceClassification.AUTOMATIC
        : ResultSlotSourceClassification.MANUAL;
  }
}
