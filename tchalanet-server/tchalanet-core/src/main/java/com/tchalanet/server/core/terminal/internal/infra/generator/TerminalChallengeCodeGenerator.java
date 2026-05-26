package com.tchalanet.server.core.terminal.internal.infra.generator;

import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeCodeGeneratorPort;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TerminalChallengeCodeGenerator implements TerminalChallengeCodeGeneratorPort {

    private static final char[] DIGITS = "0123456789".toCharArray();
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate(TerminalChallengeType challengeType) {
        var length = switch (challengeType) {
            case POS_PAIRING, ADMIN_PAIRING_CODE -> 8;
            case MOBILE_OTP -> 6;
        };
        var out = new char[length];
        for (int i = 0; i < out.length; i++) {
            out[i] = DIGITS[random.nextInt(DIGITS.length)];
        }
        return new String(out);
    }
}
