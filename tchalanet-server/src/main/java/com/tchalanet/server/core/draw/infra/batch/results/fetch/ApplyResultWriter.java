package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplyResultWriter implements ItemWriter<ApplyResultRow> {

  private final DrawReaderPort drawReaderPort;
  private final DrawResultWriterPort drawResultWriterPort;
  private final DrawWriterPort drawWriterPort;

  @Override
  public void write(Chunk<? extends ApplyResultRow> chunks) throws Exception {
    chunks
        .getItems()
        .forEach(
            row -> {
              var drawOpt = drawReaderPort.findById(row.drawId());
              if (drawOpt.isEmpty()) {
                return;
              }

              var draw = drawOpt.get();

              var dr =
                  new DrawResult(
                      DrawSource.EXTERNAL,
                      row.numbersMain(),
                      row.numbersExtra(),
                      row.occurredAt() != null ? row.occurredAt() : Instant.now(),
                      row.rawPayload() != null ? row.rawPayload().toString() : null,
                      false,
                      null);

              try {
                drawResultWriterPort.save(row.tenantId(), row.drawId(), dr);
              } catch (Exception e) {
                log.error(
                    "ApplyResultWriter: failed to save result for draw={} cause={}",
                    row.drawId(),
                    e.getMessage());
                return;
              }

              try {
                draw.applyResult(dr);
                drawWriterPort.save(draw);
              } catch (Exception e) {
                log.error(
                    "ApplyResultWriter: failed to update draw status for draw={} cause={}",
                    row.drawId(),
                    e.getMessage());
              }
            });
  }
}
