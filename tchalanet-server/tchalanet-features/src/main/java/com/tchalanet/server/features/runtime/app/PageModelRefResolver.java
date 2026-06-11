package com.tchalanet.server.features.runtime.app;

import com.tchalanet.server.features.runtime.model.PageModelRef;
import com.tchalanet.server.features.runtime.model.PrivateBootstrapSpace;
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
