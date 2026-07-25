package com.tchalanet.server.features.ops.drawresult;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.features.ops.drawresult.model.OverrideDrawResultRequest;
import com.tchalanet.server.features.ops.drawresult.model.RecordManualDrawResultRequest;
import com.tchalanet.server.features.ops.error.OpsErrorCodes;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DrawResultsOpsControllerTest {

  private final DrawResultsOpsController controller =
      new DrawResultsOpsController(null, null, null, null, null);

  @Test
  void overrideRejectsFutureDateWithStableCodeBeforeSideEffects() {
    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                controller.override(
                    new OverrideDrawResultRequest(
                        "ny_mid", LocalDate.now().plusDays(1), "123", "45", "67", "test", true)),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.DRAW_RESULT_FUTURE_DATE.code());
  }

  @Test
  void manualRejectsFutureDateWithStableCodeBeforeSideEffects() {
    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                controller.manual(
                    new RecordManualDrawResultRequest(
                        LocalDate.now().plusDays(1),
                        "ny_mid",
                        "ops@test",
                        null,
                        "123",
                        "45",
                        "67",
                        false,
                        "test")),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.DRAW_RESULT_FUTURE_DATE.code());
  }
}
