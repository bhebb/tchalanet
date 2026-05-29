package com.tchalanet.server.catalog.pagemodeltemplate.internal.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds PageModelTemplate catalog rows before tenant PageModel onboarding.
 * <p>
 * This is the missing first step:
 * <p>
 * 1. PageModelTemplateSeedRunner loads template JSON files into catalog.
 * 2. PageModelOnboardingRunner creates tenant PageModel instances from catalog templates.
 */
@Component
@RequiredArgsConstructor
@Order(10)
@Slf4j
public class PageModelTemplateSeedRunner implements ApplicationRunner {

    private final PageModelTemplateSeedService seedService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedService.seedSystemTemplates();
        } catch (Exception e) {
            log.warn("PageModel template seed skipped (non-fatal): {}", e.getMessage(), e);
        }
    }
}
