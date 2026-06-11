package com.tchalanet.server.features.runtime.app;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.runtime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.runtime.model.RuntimeReadinessCheck;
import com.tchalanet.server.features.runtime.model.RuntimeReadinessCheck.CheckStatus;
import com.tchalanet.server.features.runtime.model.RuntimeReadinessView;
import com.tchalanet.server.features.runtime.model.RuntimeReadinessView.RuntimeReadinessStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuntimeReadinessFacade {

    public RuntimeReadinessView readiness(TchRequestContext ctx, PrivateBootstrapSpace space) {
        return switch (space) {
            case PLATFORM, ADMIN -> RuntimeReadinessView.ready();
            case CASHIER -> cashierReadiness();
        };
    }

    private RuntimeReadinessView cashierReadiness() {
        // V1: operational context not yet implemented — always PARTIAL
        var checks = List.of(
            new RuntimeReadinessCheck("terminal_binding", "readiness.cashier.terminal_binding", CheckStatus.MISSING),
            new RuntimeReadinessCheck("open_session",     "readiness.cashier.open_session",     CheckStatus.MISSING),
            new RuntimeReadinessCheck("seller_assigned",  "readiness.cashier.seller_assigned",  CheckStatus.MISSING)
        );
        return new RuntimeReadinessView(RuntimeReadinessStatus.PARTIAL, checks);
    }
}
