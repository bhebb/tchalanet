package com.tchalanet.server.core.draw.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.DrawLifecycleCommandLimits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Guard for bulk draw-lifecycle commands: rejects empty input, drops null/duplicate ids, and caps
 * the batch at {@link DrawLifecycleCommandLimits#MAX_DRAW_IDS}.
 */
@DisplayName("DrawLifecycleCommandGuard.requireDrawIds")
class DrawLifecycleCommandGuardTest {

  private static DrawId id() {
    return DrawId.of(UUID.randomUUID());
  }

  @Nested
  @DisplayName("rejects empty input")
  class RejectsEmpty {

    @Test
    @DisplayName("null list → IllegalArgumentException")
    void nullList() {
      assertThatThrownBy(() -> DrawLifecycleCommandGuard.requireDrawIds(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("drawIds is required");
    }

    @Test
    @DisplayName("empty list → IllegalArgumentException")
    void emptyList() {
      assertThatThrownBy(() -> DrawLifecycleCommandGuard.requireDrawIds(List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("drawIds is required");
    }

    @Test
    @DisplayName(
        "list containing only nulls → IllegalArgumentException (nothing survives normalization)")
    void allNulls() {
      List<DrawId> onlyNulls = Arrays.asList(null, null);
      assertThatThrownBy(() -> DrawLifecycleCommandGuard.requireDrawIds(onlyNulls))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("drawIds is required");
    }
  }

  @Nested
  @DisplayName("normalizes valid input")
  class Normalizes {

    @Test
    @DisplayName("drops null entries, keeps the real ids")
    void filtersNulls() {
      DrawId a = id();
      DrawId b = id();
      List<DrawId> input = Arrays.asList(a, null, b, null);

      List<DrawId> result = DrawLifecycleCommandGuard.requireDrawIds(input);

      assertThat(result).containsExactly(a, b);
    }

    @Test
    @DisplayName("de-duplicates while preserving first-seen order")
    void deduplicates() {
      DrawId a = id();
      DrawId b = id();

      List<DrawId> result = DrawLifecycleCommandGuard.requireDrawIds(List.of(a, b, a, b, a));

      assertThat(result).containsExactly(a, b);
    }

    @Test
    @DisplayName("a clean list is returned unchanged")
    void passthrough() {
      DrawId a = id();
      DrawId b = id();

      assertThat(DrawLifecycleCommandGuard.requireDrawIds(List.of(a, b))).containsExactly(a, b);
    }
  }

  @Nested
  @DisplayName("enforces the batch cap")
  class BatchCap {

    @Test
    @DisplayName("exactly MAX_DRAW_IDS distinct ids is allowed")
    void atLimit() {
      List<DrawId> ids = distinctIds(DrawLifecycleCommandLimits.MAX_DRAW_IDS);

      List<DrawId> result = DrawLifecycleCommandGuard.requireDrawIds(ids);

      assertThat(result).hasSize(DrawLifecycleCommandLimits.MAX_DRAW_IDS);
    }

    @Test
    @DisplayName("MAX_DRAW_IDS + 1 distinct ids → IllegalArgumentException")
    void aboveLimit() {
      List<DrawId> ids = distinctIds(DrawLifecycleCommandLimits.MAX_DRAW_IDS + 1);

      assertThatThrownBy(() -> DrawLifecycleCommandGuard.requireDrawIds(ids))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(
              "cannot contain more than " + DrawLifecycleCommandLimits.MAX_DRAW_IDS);
    }

    @Test
    @DisplayName("the cap is checked AFTER de-duplication (duplicates don't count)")
    void capAppliesAfterDedup() {
      // 50 distinct ids, each repeated once → 100 entries but only 50 distinct.
      List<DrawId> distinct = distinctIds(DrawLifecycleCommandLimits.MAX_DRAW_IDS);
      List<DrawId> withDupes = new ArrayList<>(distinct);
      withDupes.addAll(distinct);

      List<DrawId> result = DrawLifecycleCommandGuard.requireDrawIds(withDupes);

      assertThat(result).hasSize(DrawLifecycleCommandLimits.MAX_DRAW_IDS);
    }

    private static List<DrawId> distinctIds(int n) {
      return IntStream.range(0, n).mapToObj(i -> id()).toList();
    }
  }
}
