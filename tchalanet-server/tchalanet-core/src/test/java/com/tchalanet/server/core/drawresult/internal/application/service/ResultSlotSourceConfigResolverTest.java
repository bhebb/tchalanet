package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.utils.JsonUtils;
import org.junit.jupiter.api.Test;

class ResultSlotSourceConfigResolverTest {

  private final ResultSlotSourceConfigResolver resolver = new ResultSlotSourceConfigResolver();

  @Test
  void resolvesPlatformReviewTrustPolicy() {
    var sourceCfg = JsonUtils.emptyObject();
    sourceCfg.put("provider_slot_code", "EVENING");
    sourceCfg.put("trust_policy", "REQUIRE_PLATFORM_REVIEW");

    var pick3 = sourceCfg.putObject("pick3");
    pick3.put("game_code", "PICK3");
    pick3.put("active", true);

    var resolved = resolver.resolve(sourceCfg);

    assertThat(resolved.providerSlotCode()).isEqualTo("EVENING");
    assertThat(resolved.requiresPlatformReview()).isTrue();
  }

  @Test
  void defaultsToProviderTrustWhenPolicyIsMissing() {
    var sourceCfg = JsonUtils.emptyObject();
    sourceCfg.put("provider_slot_code", "MIDDAY");

    var resolved = resolver.resolve(sourceCfg);

    assertThat(resolved.trustPolicy()).isEqualTo(ResultSlotSourceConfig.TrustPolicy.TRUST_PROVIDER);
    assertThat(resolved.requiresPlatformReview()).isFalse();
  }
}
