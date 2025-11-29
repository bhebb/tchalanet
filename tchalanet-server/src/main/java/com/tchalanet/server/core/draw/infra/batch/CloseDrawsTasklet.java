package com.tchalanet.server.core.draw.infra.batch;

import com.tchalanet.server.core.draw.application.port.in.command.CloseDueDrawsCommandHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Tasklet qui délègue au use case CloseDueDrawsUseCase.
 *
 * <p>Le use case se charge lui-même de: - trouver les tirages à fermer (statut OPEN, cutoff <=
 * now), - les passer à CLOSED, - gérer multi-tenant si besoin.
 */
@Component
public class CloseDrawsTasklet implements Tasklet {

  private final CloseDueDrawsCommandHandler useCase;

  public CloseDrawsTasklet(CloseDueDrawsCommandHandler useCase) {
    this.useCase = useCase;
  }

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    // Pour l’instant : on délègue tout au use case.
    // Si plus tard tu veux passer tenantId en param, tu pourras
    // le lire dans chunkContext.getStepContext().getJobParameters().
    return RepeatStatus.FINISHED;
  }
}
