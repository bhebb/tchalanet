package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordManualDrawResultCommandHandlerTest {

  @Test
  void missingResultSlotUsesStableCodeFirstProblem() {
    var slotReader = mock(ResultSlotCatalog.class);
    when(slotReader.findByKey("NY_MID")).thenReturn(Optional.empty());
    var handler = newHandler(slotReader);

    assertThatThrownBy(
            () ->
                handler.handle(
                    new RecordManualDrawResultCommand(
                        TenantId.of(UUID.randomUUID()),
                        LocalDate.of(2026, 7, 29),
                        " ny_mid ",
                        "ops@example.test",
                        null,
                        "123",
                        "45",
                        false,
                        null,
                        false)))
        .isInstanceOf(ProblemRestException.class)
        .extracting(
            error -> ((ProblemRestException) error).getProblem().getProperties().get("code"))
        .isEqualTo(DrawResultErrorCodes.RESULT_SLOT_NOT_FOUND.code());
  }

  private static RecordManualDrawResultCommandHandler newHandler(ResultSlotCatalog slotReader) {
    return new RecordManualDrawResultCommandHandler(slotReader, null, null, null, null, null, null);
  }
}
