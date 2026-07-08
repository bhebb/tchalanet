package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.haiti.api.HaitiFlags;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DrawResultPersistenceAssemblerTest {

    private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());
    private final DrawResultPersistenceAssembler assembler =
        new DrawResultPersistenceAssembler(jsonUtils);

    @Test
    void marksResultSuspectWhenPick4IsExpectedButMissing() {
        var payload = assembler.assemble(
            slot(),
            LocalDate.parse("2026-07-02"),
            Instant.parse("2026-07-03T01:00:00Z"),
            ResolvedExternalResults.of(pick3(), null, null),
            sourceCfg(true, true),
            projection(false),
            false);

        assertThat(payload.quality()).isEqualTo(ResultQuality.SUSPECT.name());
    }

    @Test
    void marksResultSuspectWhenPick3IsExpectedButMissing() {
        var payload = assembler.assemble(
            slot(),
            LocalDate.parse("2026-07-02"),
            Instant.parse("2026-07-03T01:00:00Z"),
            ResolvedExternalResults.of(null, pick4(), null),
            sourceCfg(true, true),
            projection(false),
            false);

        assertThat(payload.quality()).isEqualTo(ResultQuality.SUSPECT.name());
    }

    @Test
    void marksResultCompleteWhenAllExpectedPicksAreComplete() {
        var payload = assembler.assemble(
            slot(),
            LocalDate.parse("2026-07-02"),
            Instant.parse("2026-07-03T01:00:00Z"),
            ResolvedExternalResults.of(pick3(), pick4(), null),
            sourceCfg(true, true),
            projection(true),
            false);

        assertThat(payload.quality()).isEqualTo(ResultQuality.COMPLETE.name());
    }

    private ResultSlotView slot() {
        return new ResultSlotView(
            ResultSlotId.of(UUID.randomUUID()),
            "NY_EVE",
            "US_LOTTERY",
            ZoneId.of("America/New_York"),
            LocalTime.of(21, 0),
            "MON,TUE,WED,THU,FRI,SAT,SUN",
            true,
            JsonUtils.emptyObject(),
            JsonUtils.emptyObject(),
            "draw_channel.ny.eve.label");
    }

    private ResultSlotSourceConfig sourceCfg(boolean pick3Active, boolean pick4Active) {
        return new ResultSlotSourceConfig(
            "NYEVE",
            new ResultSlotSourceConfig.SourceGame("PICK3", pick3Active),
            new ResultSlotSourceConfig.SourceGame("PICK4", pick4Active));
    }

    private HaitiProjectionResult projection(boolean ok) {
        return new HaitiProjectionResult(
            JsonUtils.emptyObject(),
            ok
                ? HaitiFlags.ok(1, Instant.parse("2026-07-03T01:05:00Z"))
                : HaitiFlags.fail(1, "PARTIAL_EXTERNAL_PICK", Map.of()));
    }

    private ExternalResultItem pick3() {
        return new ExternalResultItem(
            "PICK3",
            List.of("1", "2", "3"),
            List.of(),
            ResultQuality.COMPLETE,
            sourceFlags("pick3"),
            Instant.parse("2026-07-03T01:00:00Z"),
            null);
    }

    private ExternalResultItem pick4() {
        return new ExternalResultItem(
            "PICK4",
            List.of("4", "5", "6", "7"),
            List.of(),
            ResultQuality.COMPLETE,
            sourceFlags("pick4"),
            Instant.parse("2026-07-03T01:00:00Z"),
            null);
    }

    private ExternalSourceFlags sourceFlags(String game) {
        return new ExternalSourceFlags(
            "TEST",
            game + "-hash",
            "https://example.test/" + game,
            Map.of());
    }
}
