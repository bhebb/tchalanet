package com.tchalanet.server.core.outlet.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockStateTest {

    private static final Instant NOW = Instant.parse("2025-01-15T10:00:00Z");
    private static final UserId ACTOR = UserId.of(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));

    @Nested
    class None {
        @Test
        void noneIsUnblocked() {
            var state = BlockState.none();
            assertThat(state.blocked()).isFalse();
            assertThat(state.reason()).isNull();
            assertThat(state.at()).isNull();
            assertThat(state.by()).isNull();
        }
    }

    @Nested
    class Block {
        @Test
        void blockSetsAllFields() {
            var state = BlockState.none().block("fraud_check", NOW, ACTOR);
            assertThat(state.blocked()).isTrue();
            assertThat(state.reason()).isEqualTo("fraud_check");
            assertThat(state.at()).isEqualTo(NOW);
            assertThat(state.by()).isEqualTo(ACTOR);
        }

        @Test
        void blockWithNullReasonIsAllowed() {
            var state = BlockState.none().block(null, NOW, null);
            assertThat(state.blocked()).isTrue();
            assertThat(state.reason()).isNull();
        }

        @Test
        void blockingAlreadyBlockedStateOverwritesReason() {
            var first = BlockState.none().block("reason_1", NOW, ACTOR);
            var second = first.block("reason_2", NOW.plusSeconds(60), ACTOR);
            assertThat(second.blocked()).isTrue();
            assertThat(second.reason()).isEqualTo("reason_2");
        }
    }

    @Nested
    class Unblock {
        @Test
        void unblockClearsAllFields() {
            var state = BlockState.none().block("reason", NOW, ACTOR).unblock();
            assertThat(state.blocked()).isFalse();
            assertThat(state.reason()).isNull();
            assertThat(state.at()).isNull();
            assertThat(state.by()).isNull();
        }

        @Test
        void unblockOnNoneReturnsNone() {
            var state = BlockState.none().unblock();
            assertThat(state).isEqualTo(BlockState.none());
        }
    }
}
