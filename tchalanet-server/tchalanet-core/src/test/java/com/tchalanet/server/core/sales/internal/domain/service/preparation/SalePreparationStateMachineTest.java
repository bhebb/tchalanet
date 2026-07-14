package com.tchalanet.server.core.sales.internal.domain.service.preparation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SalePreparationStateMachine")
class SalePreparationStateMachineTest {

  private final SalePreparationStateMachine machine = new SalePreparationStateMachine();

  @Test
  @DisplayName("DRAFT accepts CONFIRM, EXPIRE, CANCEL")
  void draftTransitions() {
    assertThat(machine.apply(SalePreparationStatus.DRAFT, SalePreparationTransition.CONFIRM))
        .isEqualTo(SalePreparationStatus.CONFIRMED);
    assertThat(machine.apply(SalePreparationStatus.DRAFT, SalePreparationTransition.EXPIRE))
        .isEqualTo(SalePreparationStatus.EXPIRED);
    assertThat(machine.apply(SalePreparationStatus.DRAFT, SalePreparationTransition.CANCEL))
        .isEqualTo(SalePreparationStatus.CANCELLED);
  }

  @Test
  @DisplayName("terminal states reject every transition")
  void terminalStatesRejectAll() {
    for (var status :
        new SalePreparationStatus[] {
          SalePreparationStatus.CONFIRMED,
          SalePreparationStatus.EXPIRED,
          SalePreparationStatus.CANCELLED
        }) {
      for (var transition : SalePreparationTransition.values()) {
        assertThatThrownBy(() -> machine.apply(status, transition))
            .isInstanceOf(TchConflictException.class);
      }
    }
  }

  @Test
  @DisplayName("null status is rejected")
  void nullStatusRejected() {
    assertThatThrownBy(() -> machine.apply(null, SalePreparationTransition.CONFIRM))
        .isInstanceOf(TchConflictException.class);
  }
}
