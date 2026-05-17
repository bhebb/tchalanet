package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.DispatchReadyOfflineSubmissionsCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.sales.api.command.offline.ProcessOfflineSubmissionForSalesCommand;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@UseCase
@RequiredArgsConstructor
public class DispatchReadyOfflineSubmissionsCommandHandler
    implements CommandHandler<DispatchReadyOfflineSubmissionsCommand, Integer> {

    private final OfflineSubmissionReaderPort reader;
    private final OfflineSubmissionWriterPort writer;
    private final CommandBus commandBus;
    private final Clock clock;

    @Value("${tch.offlinesync.sales-processing.max-items-per-tick:500}")
    private int maxItemsPerTick;

    @Override
    @TchTx
    public Integer handle(DispatchReadyOfflineSubmissionsCommand command) {
        var now = Instant.now(clock);
        var submissions = reader.findReadyForDispatch(command.tenantId(), maxItemsPerTick, now);

        if (submissions.isEmpty()) {
            return 0;
        }

        var claimed = submissions.stream()
            .map(s -> s.withStatus(OfflineSubmissionStatus.SALES_PROCESSING))
            .toList();

        writer.saveAll(claimed);

        claimed.forEach(submission ->
            commandBus.execute(new ProcessOfflineSubmissionForSalesCommand(
                submission.tenantId(),
                submission.id())));

        return claimed.size();
    }
}
