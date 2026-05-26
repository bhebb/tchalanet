package com.tchalanet.server.core.terminal.internal.application.port.out.challenge;

import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;

public interface TerminalActivationChallengeWriterPort {

    TerminalActivationChallenge save(TerminalActivationChallenge challenge);
}
