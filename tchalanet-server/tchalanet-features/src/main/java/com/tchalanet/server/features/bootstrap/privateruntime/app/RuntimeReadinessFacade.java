package com.tchalanet.server.features.bootstrap.privateruntime.app;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeReadinessCheck;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeReadinessView;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace.ADMIN;
import static com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace.CASHIER;
import static com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace.PLATFORM;


@Component
public class RuntimeReadinessFacade {

    public RuntimeReadinessView readiness(TchRequestContext ctx, PrivateBootstrapSpace space) {
        return switch (space) {
            case PLATFORM, ADMIN -> RuntimeReadinessView.ready();
            case CASHIER -> cashierReadiness();
        };
    }

    private RuntimeReadinessView cashierReadiness() {
        var checks = List.of(
            new RuntimeReadinessCheck("seller_terminal", "readiness.cashier.seller_terminal", RuntimeReadinessCheck.CheckStatus.MISSING)
        );
        return new RuntimeReadinessView(RuntimeReadinessView.RuntimeReadinessStatus.PARTIAL, checks);
    }
}
