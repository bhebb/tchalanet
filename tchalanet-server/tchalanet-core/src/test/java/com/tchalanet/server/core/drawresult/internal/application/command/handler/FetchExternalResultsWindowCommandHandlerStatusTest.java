package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultSlotSourceConfig;
import org.junit.jupiter.api.Test;

class FetchExternalResultsWindowCommandHandlerStatusTest {

  @Test
  void confirmsCompleteTrustedProviderResult() {
    assertThat(
            FetchExternalResultsWindowCommandHandler.resolvePersistStatus(
                sourceCfg(ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER),
                ResultQuality.COMPLETE.name()))
        .isEqualTo(DrawResultStatus.CONFIRMED);
  }

  @Test
  void keepsCompleteResultProvisionalWhenPlatformReviewIsRequired() {
    assertThat(
            FetchExternalResultsWindowCommandHandler.resolvePersistStatus(
                sourceCfg(ResultSlotSourceConfig.TrustPolicy.REQUIRE_PLATFORM_REVIEW),
                ResultQuality.COMPLETE.name()))
        .isEqualTo(DrawResultStatus.PROVISIONAL);
  }

  @Test
  void keepsSuspectResultProvisionalEvenWhenProviderIsTrusted() {
    assertThat(
            FetchExternalResultsWindowCommandHandler.resolvePersistStatus(
                sourceCfg(ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER),
                ResultQuality.SUSPECT.name()))
        .isEqualTo(DrawResultStatus.PROVISIONAL);
  }

  @Test
  void keepsInvalidResultProvisionalEvenWhenProviderIsTrusted() {
    assertThat(
            FetchExternalResultsWindowCommandHandler.resolvePersistStatus(
                sourceCfg(ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER),
                ResultQuality.INVALID.name()))
        .isEqualTo(DrawResultStatus.PROVISIONAL);
  }

  @Test
  void keepsUnknownQualityProvisional() {
    assertThat(
            FetchExternalResultsWindowCommandHandler.resolvePersistStatus(
                sourceCfg(ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER), "partial"))
        .isEqualTo(DrawResultStatus.PROVISIONAL);
  }

  private ResultSlotSourceConfig sourceCfg(ResultSlotSourceConfig.TrustPolicy trustPolicy) {
    return new ResultSlotSourceConfig(
        "MIDDAY",
        new ResultSlotSourceConfig.SourceGame("PICK3", true),
        new ResultSlotSourceConfig.SourceGame("PICK4", true),
        trustPolicy);
  }
}
