package com.tchalanet.server.core.session.internal.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.session.api.query.PosOperationAction;
import org.junit.jupiter.api.Test;

class PosActionPolicyTest {

    private final PosActionPolicy policy = new PosActionPolicy();

    @Test
    void coversAllActions() {
        for (var action : PosOperationAction.values()) {
            assertThatCode(() -> policy.terminalOperation(action)).doesNotThrowAnyException();
            assertThatCode(() -> policy.outletOperation(action)).doesNotThrowAnyException();
            assertThatCode(() -> policy.salesSessionOperation(action)).doesNotThrowAnyException();
        }
    }

    @Test
    void offlineGrantRejectsWeakTrust() {
        assertThatThrownBy(() -> policy.assertAccepted(
            PosOperationAction.REQUEST_OFFLINE_GRANT,
            OperationalContextTrust.WEAK))
            .isInstanceOf(ProblemRestException.class);
    }

    @Test
    void adminPosSellAcceptsWeakTrust() {
        assertThatCode(() -> policy.assertAccepted(
            PosOperationAction.ADMIN_POS_SELL,
            OperationalContextTrust.WEAK))
            .doesNotThrowAnyException();
    }
}
