package com.tchalanet.server.core.terminal.internal.application.port.out.challenge;

import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDelivery;

public interface TerminalChallengeDeliveryPort {

    TerminalChallengeDelivery deliver(TerminalActivationChallenge challenge, String clearCode);
}
