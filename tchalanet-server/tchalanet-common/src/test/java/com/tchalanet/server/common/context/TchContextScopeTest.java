package com.tchalanet.server.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TchContextScope")
class TchContextScopeTest {

  @AfterEach
  void cleanup() {
    TchContext.clear();
  }

  @Nested
  @DisplayName("runWithContext")
  class RunWithContext {

    @Test
    @DisplayName("should set context during execution and restore previous after")
    void shouldSetAndRestorePrevious() {
      var previous = TchRequestContext.startupTenant(UUID.randomUUID(), "prev");
      TchContext.set(previous);

      var scoped = TchRequestContext.startupTenant(UUID.randomUUID(), "scoped");

      TchContextScope.runWithContext(
          scoped,
          () -> {
            assertThat(TchContext.currentOrNull()).isSameAs(scoped);
          });

      assertThat(TchContext.currentOrNull()).isSameAs(previous);
    }

    @Test
    @DisplayName("should clear context after if no previous existed")
    void shouldClearIfNoPrevious() {
      assertThat(TchContext.currentOrNull()).isNull();

      var scoped = TchRequestContext.startupTenant(UUID.randomUUID(), "scoped");

      TchContextScope.runWithContext(
          scoped,
          () -> {
            assertThat(TchContext.hasContext()).isTrue();
          });

      assertThat(TchContext.hasContext()).isFalse();
    }

    @Test
    @DisplayName("should restore previous even if work throws")
    void shouldRestoreOnException() {
      var previous = TchRequestContext.startupTenant(UUID.randomUUID(), "prev");
      TchContext.set(previous);

      var scoped = TchRequestContext.startupTenant(UUID.randomUUID(), "scoped");

      assertThatThrownBy(
              () ->
                  TchContextScope.runWithContext(
                      scoped,
                      () -> {
                        throw new RuntimeException("boom");
                      }))
          .hasMessage("boom");

      assertThat(TchContext.currentOrNull()).isSameAs(previous);
    }
  }

  @Nested
  @DisplayName("runWithContextResult")
  class RunWithContextResult {

    @Test
    @DisplayName("should return result and restore context")
    void shouldReturnResultAndRestore() {
      var scoped = TchRequestContext.startupTenant(UUID.randomUUID(), "scoped");

      var result =
          TchContextScope.runWithContextResult(
              scoped,
              () -> {
                assertThat(TchContext.currentOrNull()).isSameAs(scoped);
                return 42;
              });

      assertThat(result).isEqualTo(42);
      assertThat(TchContext.hasContext()).isFalse();
    }
  }

  @Nested
  @DisplayName("runStartupTenant")
  class RunStartupTenant {

    @Test
    @DisplayName("should bind a SYSTEM context with the given tenant UUID")
    void shouldBindSystemContext() {
      var tenantUuid = UUID.randomUUID();

      TchContextScope.runStartupTenant(
          tenantUuid,
          "init",
          () -> {
            var ctx = TchContext.currentOrThrow();
            assertThat(ctx.tenantUuid()).isEqualTo(tenantUuid);
            assertThat(ctx.actorType()).isEqualTo(TchActorType.SYSTEM);
            assertThat(ctx.requestId()).isEqualTo("init");
          });
    }

    @Test
    @DisplayName("should default requestId to 'startup' when null")
    void shouldDefaultRequestId() {
      TchContextScope.runStartupTenant(
          UUID.randomUUID(),
          null,
          () -> {
            assertThat(TchContext.currentOrThrow().requestId()).isEqualTo("startup");
          });
    }
  }
}
