package com.tchalanet.server.core.terminal.internal.application.port.out.challenge;

import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;

public interface TerminalChallengeCodeGeneratorPort {

    String generate(TerminalChallengeType challengeType);
}
