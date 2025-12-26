package com.tchalanet.server.core.draw.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettleWriter implements ItemWriter<DrawId> {

    private final DrawReaderPort drawReaderPort;
    private final DrawWriterPort drawWriterPort;

    @Override
    public void write(Chunk<? extends DrawId> chunks) throws Exception {
        chunks.getItems()
            .forEach(drawId -> {
                var drawOpt = drawReaderPort.findById(drawId);
                if (drawOpt.isEmpty()) return;

                var draw = drawOpt.get();
                try {
                    // only settle if status is RESULTED
                    if (draw.status() != com.tchalanet.server.core.draw.domain.model.DrawStatus.RESULTED) return;

                    // call domain method settle() and persist via writer port
                    draw.settle();
                    drawWriterPort.save(draw);
                } catch (Exception e) {
                    log.error("SettleWriter: failed to settle draw={} cause={}", drawId, e.getMessage());
                }
            });
    }
}
