package com.tchalanet.server.core.haiti.internal.domain.tchala.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.core.haiti.internal.domain.tchala.model.MergePolicy;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaNumber;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TchalaMerge")
class TchalaMergeTest {

  private static final TchalaNumber N7 = TchalaNumber.of(7);
  private static final TchalaNumber N12 = TchalaNumber.of(12);
  private static final TchalaNumber N42 = TchalaNumber.of(42);
  private static final TchalaNumber N88 = TchalaNumber.of(88);

  @Nested
  @DisplayName("UNION_NUMBERS")
  class Union {

    @Test
    @DisplayName("merges and deduplicates, sorted by value")
    void mergesAndDeduplicates() {
      var result =
          TchalaMerge.mergeNumbers(List.of(N42, N7), List.of(N88, N7), MergePolicy.UNION_NUMBERS);
      assertThat(result).containsExactly(N7, N42, N88);
    }

    @Test
    @DisplayName("handles empty canonical")
    void emptyCanonical() {
      var result = TchalaMerge.mergeNumbers(List.of(), List.of(N12, N7), MergePolicy.UNION_NUMBERS);
      assertThat(result).containsExactly(N7, N12);
    }

    @Test
    @DisplayName("handles empty pending")
    void emptyPending() {
      var result = TchalaMerge.mergeNumbers(List.of(N42), List.of(), MergePolicy.UNION_NUMBERS);
      assertThat(result).containsExactly(N42);
    }
  }

  @Nested
  @DisplayName("CANONICAL_WINS")
  class CanonicalWins {

    @Test
    @DisplayName("returns only canonical numbers sorted")
    void returnsCanonicalSorted() {
      var result =
          TchalaMerge.mergeNumbers(List.of(N42, N7), List.of(N88), MergePolicy.CANONICAL_WINS);
      assertThat(result).containsExactly(N7, N42);
    }
  }

  @Nested
  @DisplayName("PENDING_WINS")
  class PendingWins {

    @Test
    @DisplayName("returns only pending numbers sorted")
    void returnsPendingSorted() {
      var result =
          TchalaMerge.mergeNumbers(List.of(N42), List.of(N88, N12), MergePolicy.PENDING_WINS);
      assertThat(result).containsExactly(N12, N88);
    }
  }

  @Test
  @DisplayName("null canonical throws NPE")
  void nullCanonical() {
    assertThatThrownBy(() -> TchalaMerge.mergeNumbers(null, List.of(), MergePolicy.UNION_NUMBERS))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("null pending throws NPE")
  void nullPending() {
    assertThatThrownBy(() -> TchalaMerge.mergeNumbers(List.of(), null, MergePolicy.UNION_NUMBERS))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("null policy throws NPE")
  void nullPolicy() {
    assertThatThrownBy(() -> TchalaMerge.mergeNumbers(List.of(), List.of(), null))
        .isInstanceOf(NullPointerException.class);
  }
}
