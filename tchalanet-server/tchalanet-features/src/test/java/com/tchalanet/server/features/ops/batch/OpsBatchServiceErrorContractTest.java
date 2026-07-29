package com.tchalanet.server.features.ops.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.history.BatchJobHistoryService;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobExecutionOperator;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.features.ops.batch.model.GateUpdateRequest;
import com.tchalanet.server.features.ops.batch.model.PurgeExecutionsRequest;
import com.tchalanet.server.features.ops.error.OpsErrorCodes;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpsBatchServiceErrorContractTest {

  private final TchJobRegistry registry = mock(TchJobRegistry.class);
  private final BatchJobHistoryService history = mock(BatchJobHistoryService.class);
  private final OpsBatchService service =
      new OpsBatchService(
          registry,
          mock(BatchGate.class),
          mock(BatchJobStarter.class),
          mock(BatchJobExecutionOperator.class),
          history,
          mock(SettingRepository.class));

  @Test
  void rejectsMalformedJobKeyWithStableCode() {
    var exception =
        catchThrowableOfType(
            () -> service.getJob("not-a-job-key"), ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_JOB_KEY_INVALID.code());
  }

  @Test
  void rejectsUnknownJobWithStableNotFoundCode() {
    when(registry.find(JobKey.of("draw:lifecycle:missing"))).thenReturn(Optional.empty());

    var exception =
        catchThrowableOfType(
            () -> service.getJob("draw:lifecycle:missing"), ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_JOB_NOT_FOUND.code());
  }

  @Test
  void rejectsInvalidGateScopeWithStableCode() {
    when(registry.find(any(JobKey.class))).thenReturn(Optional.of(registeredJob()));

    var exception =
        catchThrowableOfType(
            () ->
                service.updateGate(
                    "draw:lifecycle:open", new GateUpdateRequest("ACCOUNT", null, true, null)),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_SCOPE_INVALID.code());
  }

  @Test
  void rejectsMissingExecutionWithStableNotFoundCode() {
    when(history.getExecution(42L)).thenReturn(Optional.empty());

    var exception =
        catchThrowableOfType(
            () -> service.getExecution(42L), ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_EXECUTION_NOT_FOUND.code());
  }

  @Test
  void rejectsInvalidRetentionWithStableCode() {
    var exception =
        catchThrowableOfType(
            () -> service.purgeExecutions(new PurgeExecutionsRequest(0)), ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", OpsErrorCodes.BATCH_RETENTION_INVALID.code());
  }

  private static RegisteredJob registeredJob() {
    return new RegisteredJob(
        JobKey.of("draw:lifecycle:open"),
        "Open draws",
        RegisteredJob.JobScope.GLOBAL,
        java.util.Set.of(),
        java.util.Set.of());
  }
}
