package com.tchalanet.server.core.draw.infra.batch.results.fetch;
import com.tchalanet.server.common.types.id.DrawId;

import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class FetchExternalResultProcessor implements ItemProcessor<DrawId, ApplyResultRow> {

    private final DrawReaderPort drawReaderPort;
    private final ExternalDrawResultPort externalDrawResultPort;

    @Value("#{jobParameters['force']}")
    private String forceFlag;

    @Override
    public ApplyResultRow process(DrawId drawId) throws Exception {
        var drawOpt = drawReaderPort.findById(drawId);
        if (drawOpt.isEmpty()) {
            return null;
        }
        var draw = drawOpt.get();

        var force = "true".equalsIgnoreCase(forceFlag);
        if (!force && draw.status() == com.tchalanet.server.core.draw.domain.model.DrawStatus.RESULTED && draw.result() != null) {
            return null;
        }

        var zone = draw.drawChannel().timezone();
        var drawDateLocal = draw.scheduledAt().withZoneSameInstant(zone).toLocalDate();
        var executedAt = Instant.now();

        var query = new ExternalDrawResultPort.DrawExternalQuery(draw.drawChannel().code(), drawDateLocal, executedAt, force);

        var result = externalDrawResultPort.fetchExternalResult(query);
        if (result == null || !result.found()) {
            return null;
        }

        return new ApplyResultRow(draw.tenantId(), draw.id(), result.numbers(), result.numbersExtra(), result.occurredAt(), result.rawPayload());
    }
}
