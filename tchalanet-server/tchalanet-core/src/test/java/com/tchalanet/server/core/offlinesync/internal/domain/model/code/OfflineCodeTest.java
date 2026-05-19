package com.tchalanet.server.core.offlinesync.internal.domain.model.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OfflineCode")
class OfflineCodeTest {

    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
    private static final OfflineCodeId CODE_ID = OfflineCodeId.of(UUID.randomUUID());
    private static final OfflineCodeBatchId BATCH_ID = OfflineCodeBatchId.of(UUID.randomUUID());
    private static final OfflineGrantId GRANT_ID = OfflineGrantId.of(UUID.randomUUID());
    private static final OfflineSubmissionId SUBMISSION_ID = OfflineSubmissionId.of(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(3600);

    private OfflineCode newAvailable() {
        return OfflineCode.issue(CODE_ID, TENANT, BATCH_ID, GRANT_ID, "ABC123", EXPIRY);
    }

    @Nested
    @DisplayName("reservation lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("AVAILABLE -> RESERVED records submissionId and reservedAt")
        void availableToReserved() {
            var reserved = newAvailable().reserve(SUBMISSION_ID, NOW);

            assertThat(reserved.status()).isEqualTo(OfflineCodeStatus.RESERVED);
            assertThat(reserved.lifecycle().reservedAt()).isEqualTo(NOW);
            assertThat(reserved.lifecycle().offlineSubmissionId()).isEqualTo(SUBMISSION_ID);
        }

        @Test
        @DisplayName("RESERVED -> CONSUMED_REJECTED on technical rejection")
        void reservedToConsumedRejected() {
            var rejected = newAvailable().reserve(SUBMISSION_ID, NOW)
                .markConsumedRejected(NOW.plusSeconds(1));

            assertThat(rejected.status()).isEqualTo(OfflineCodeStatus.CONSUMED_REJECTED);
            assertThat(rejected.lifecycle().ticketId()).isNull();
        }

        @Test
        @DisplayName("RESERVED -> AVAILABLE is forbidden — invariant of the spec v2.1")
        void cannotReturnReservedToAvailable() {
            var reserved = newAvailable().reserve(SUBMISSION_ID, NOW);

            assertThatThrownBy(() -> reserved.reserve(SUBMISSION_ID, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be reserved from status RESERVED");
        }

        @Test
        @DisplayName("CONSUMED_PROMOTED cannot transition further")
        void terminalStateIsTerminal() {
            var promoted = newAvailable().reserve(SUBMISSION_ID, NOW)
                .markConsumedPromoted(TicketId.of(UUID.randomUUID()), NOW.plusSeconds(2));

            assertThatThrownBy(() -> promoted.markConsumedRejected(NOW.plusSeconds(3)))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("expired code refuses reservation")
        void expiredCodeRefusesReservation() {
            var code = newAvailable();
            var past = EXPIRY.plusSeconds(1);

            assertThatThrownBy(() -> code.reserve(SUBMISSION_ID, past))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        }
    }
}
