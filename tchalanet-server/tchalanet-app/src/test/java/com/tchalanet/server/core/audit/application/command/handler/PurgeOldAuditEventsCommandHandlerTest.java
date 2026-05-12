package com.tchalanet.server.core.audit.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.audit.application.command.model.PurgeOldAuditEventsCommand;
import com.tchalanet.server.core.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PurgeOldAuditEventsCommandHandlerTest {

    @Test
    void purgeUsesRetentionDaysAndInjectedClockForDeterministicThreshold() {
        var writer = new CapturingWriter(7);
        var clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);
        var handler = new PurgeOldAuditEventsCommandHandler(writer, clock);
        ReflectionTestUtils.setField(handler, "retentionDays", 90);

        var result = handler.handle(new PurgeOldAuditEventsCommand());

        assertThat(writer.threshold).isEqualTo(Instant.parse("2026-01-29T00:00:00Z"));
        assertThat(result.deleted()).isEqualTo(7);
        assertThat(result.retentionDays()).isEqualTo(90);
        assertThat(result.threshold()).isEqualTo(writer.threshold);
    }

    private static final class CapturingWriter implements AuditEventWriterPort {
        private final int deleted;
        private Instant threshold;

        private CapturingWriter(int deleted) {
            this.deleted = deleted;
        }

        @Override
        public AuditEvent save(AuditEvent event) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public int deleteBefore(Instant threshold) {
            this.threshold = threshold;
            return deleted;
        }
    }
}
