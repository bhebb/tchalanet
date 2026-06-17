package com.tchalanet.server.features.bootstrap;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.bootstrap.RuntimeReadinessView.RuntimeReadinessStatus;
import org.springframework.stereotype.Component;

import java.util.List;

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
            new RuntimeReadinessCheck("terminal_binding", "readiness.cashier.terminal_binding", RuntimeReadinessCheck.CheckStatus.MISSING),
            new RuntimeReadinessCheck("open_session",     "readiness.cashier.open_session",     RuntimeReadinessCheck.CheckStatus.MISSING),
            new RuntimeReadinessCheck("seller_assigned",  "readiness.cashier.seller_assigned",  RuntimeReadinessCheck.CheckStatus.MISSING)
        );
        return new RuntimeReadinessView(RuntimeReadinessStatus.PARTIAL, checks);
    }
}
