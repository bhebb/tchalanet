package com.tchalanet.server.common.tx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("AfterCommit")
class AfterCommitTest {

  @Nested
  @DisplayName("when no transaction is active")
  class NoTransaction {

    @Test
    @DisplayName("should run the callback immediately")
    void shouldRunImmediately() {
      var ran = new AtomicBoolean(false);

      AfterCommit.run(() -> ran.set(true));

      assertThat(ran).isTrue();
    }
  }

  @Nested
  @DisplayName("when a transaction is active")
  class WithTransaction {

    @Test
    @DisplayName("should defer the callback until afterCommit")
    void shouldDeferUntilAfterCommit() {
      var ran = new AtomicBoolean(false);

      TransactionSynchronizationManager.initSynchronization();
      try {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        AfterCommit.run(() -> ran.set(true));

        assertThat(ran).as("should not run before commit").isFalse();

        var syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);

        syncs.forEach(TransactionSynchronization::afterCommit);

        assertThat(ran).as("should run after commit").isTrue();
      } finally {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.clearSynchronization();
      }
    }
  }
}
