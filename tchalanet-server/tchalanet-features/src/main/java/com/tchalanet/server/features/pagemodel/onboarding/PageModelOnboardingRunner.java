package com.tchalanet.server.features.pagemodel.onboarding;

import com.tchalanet.server.common.constant.CommonConstants;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs at startup after PageModelTemplateSeedRunner (@Order(10)).
 * Seeds default PageModel instances from templates for the default tenant.
 */
@Component
@RequiredArgsConstructor
@Order(20)
@Slf4j
public class PageModelOnboardingRunner implements ApplicationRunner {

  private final PageModelOnboardingService onboardingService;

  @Override
  public void run(@Nonnull ApplicationArguments args) {
    try {
      TchContextScope.runStartupTenant(
          CommonConstants.DEFAULT_TENANT_UUID,
          "pagemodel:onboarding",
          onboardingService::seedDefaultsForDefaultTenant);
    } catch (Exception e) {
      log.warn("PageModel onboarding skipped (non-fatal): {}", e.getMessage());
    }
  }
}
