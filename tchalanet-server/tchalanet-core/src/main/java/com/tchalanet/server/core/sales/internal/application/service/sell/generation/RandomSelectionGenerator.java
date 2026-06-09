package com.tchalanet.server.core.sales.internal.application.service.sell.generation;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * RANDOM strategy: uniform draw over the game's selection space.
 * <p>
 * Output is a raw selection string accepted by
 * {@code SelectionKeyCanonicalizer} for the given bet type; callers must still
 * canonicalize via {@code SelectionApi}.
 */
@Component
public class RandomSelectionGenerator {

    private final RandomGenerator random;

    public RandomSelectionGenerator() {
        this(new SecureRandom());
    }

    RandomSelectionGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String generateRaw(BetType betType, Short betOption) {
        return switch (betType) {
            case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> digits(2);
            case MARRIAGE_2D2D -> distinctTwoDigitPair();
            case LOTTO3_3D -> digits(3);
            case LOTTO4_PATTERN -> lotto4(betOption);
            case LOTTO5_PATTERN -> digits(5);
        };
    }

    /**
     * Two distinct 2-digit numbers ("a-b"). A maryaj of a number with itself
     * is rejected by business rule, even though canonicalization accepts it.
     */
    private String distinctTwoDigitPair() {
        int a = random.nextInt(100);
        int b = random.nextInt(99);
        if (b >= a) {
            b++;
        }
        return "%02d-%02d".formatted(a, b);
    }

    private String lotto4(Short betOption) {
        BetOption option = BetOption.from(BetType.LOTTO4_PATTERN, betOption);
        return switch (option) {
            case LOTTO4_STRAIGHT, LOTTO4_BOX -> digits(4);
            case LOTTO4_FRONT_PAIR -> digits(2) + "**";
            case LOTTO4_BACK_PAIR -> "**" + digits(2);
            default -> throw new IllegalArgumentException(
                "selection.generation.unsupported_bet_option: " + option
            );
        };
    }

    private String digits(int width) {
        var sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
