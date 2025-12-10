package com.tchalanet.features.stats.aggregates.application;

import com.tchalanet.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RecomputeDailyStatsUseCaseTest {

    @Test
    void handle_should_call_delete_on_repo() {
        StatsDailyJpaRepository repo = Mockito.mock(StatsDailyJpaRepository.class);
        RecomputeDailyStatsUseCase useCase = new RecomputeDailyStatsUseCase(repo);

        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();
        UUID tenantId = UUID.randomUUID();

        RecomputeDailyStatsCommand cmd = new RecomputeDailyStatsCommand(from, to, tenantId);
        useCase.handle(cmd);

        verify(repo, times(1)).deleteByRefDateBetween(from, to);
    }
}

