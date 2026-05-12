package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultsFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExternalResultFetcher {

    private final ExternalResultsFetchPort fetchPort;

    public ResolvedExternalResults fetch(
        FetchExternalResultsWindowCommand cmd,
        ResultSlotView slot,
        ResultSlotSourceConfig sourceCfg,
        LocalDate date,
        Instant now) {

        if (sourceCfg == null || !sourceCfg.hasAnyActiveGame()) {
            return ResolvedExternalResults.empty();
        }

        var bundle =
            fetchPort.fetchProviderResults(
                new ExternalResultsFetchPort.ExternalResultFetchQuery(
                    slot.provider(),
                    date,
                    slot.drawTime(),
                    slot.timezone(),
                    sourceCfg.activeGameCodes(),
                    cmd.force(),
                    cmd.includeRaw(),
                    now));

        if (bundle == null || !bundle.hasAnyResult()) {
            return ResolvedExternalResults.empty();
        }

        var p3 = sourceCfg.activePick3()
            .map(cfg -> bundle.findByGameCode(cfg.gameCode()))
            .orElse(null);

        var p4 = sourceCfg.activePick4()
            .map(cfg -> bundle.findByGameCode(cfg.gameCode()))
            .orElse(null);

        return ResolvedExternalResults.of(p3, p4, bundle.rawPayload());
    }
}
