package com.tchalanet.server.core.pagemodel.infra.init;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchContextRunner;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(20)
public class PageModelStartupInitializer implements ApplicationRunner {

    private final PageModelBootstrapService bootstrapService;

    @Override
    public void run(@Nonnull ApplicationArguments args) {

        TchContextRunner.runAsTenant(CommonConstants.DEFAULT_TENANT_UUID, "startup",
            bootstrapService::seedDefaultsForTenant
        );
    }
}
