package com.tchalanet.server.features.bootstrap;

import org.springframework.stereotype.Component;

@Component
public class PageModelRefResolver {

    public PageModelRef resolve(PrivateBootstrapSpace space) {
        return switch (space) {
            case PLATFORM -> new PageModelRef("/app/platform", "/platform/dashboard");
            case ADMIN    -> new PageModelRef("/app/admin",    "/tenant/dashboard");
            case CASHIER  -> new PageModelRef("/app/cashier",  "/tenant/dashboard");
        };
    }
}
