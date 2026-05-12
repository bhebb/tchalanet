package com.tchalanet.server.core.sales.internal.infra.generator;

import com.tchalanet.server.core.sales.application.port.out.TicketNumberGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates an internal ticket code intended for OPS/support/search.
 *
 * Why:
 * - Human-friendly prefix and time component for debugging/troubleshooting
 * - Random suffix to make collisions extremely unlikely
 * - Check digit to catch manual input mistakes
 *
 * Format: TCK-YYMMDD-HHMMSS-XXXXXX-C
 * Example: TCK-260113-214501-9K3W2H-7
 */
@Component
public class TimeBasedTicketNumberGenerator implements TicketNumberGeneratorPort {

    // Crockford Base32 avoids ambiguous chars (I,L,O,U)
    private static final String BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final SecureRandom RNG = new SecureRandom();

    // Keep it in UTC to avoid timezone surprises in logs/support
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    private final Clock clock;

    public TimeBasedTicketNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String generate() {
        Instant now = Instant.now(clock);

        // 6 base32 chars ~ 30 bits of randomness
        String rand = randomBase32(6);

        String core = "TCK-" + DATE.format(now) + "-" + TIME.format(now) + "-" + rand;

        // Add a check digit to detect typing mistakes
        char check = computeCheckDigit(core);

        return core + "-" + check;
    }

    private static String randomBase32(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(BASE32.charAt(RNG.nextInt(BASE32.length())));
        }
        return sb.toString();
    }

    /**
     * Simple mod-10 check digit (not crypto).
     * Goal: catch common mistakes when staff types the ticket code.
     */
    private static char computeCheckDigit(String s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            sum = (sum + s.charAt(i)) % 10;
        }
        return (char) ('0' + sum);
    }
}
