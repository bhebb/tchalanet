package com.tchalanet.server.features.pagemodel.shared.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageModelStartupInitializer implements ApplicationRunner {

    private final PageModelBootstrapService bootstrapService;

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.seedDefaultsForDefaultTenant();
    }
}

