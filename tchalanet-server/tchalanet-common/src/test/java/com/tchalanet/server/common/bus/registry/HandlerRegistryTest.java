package com.tchalanet.server.common.bus.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.HandlerTypeResolver;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.bus.exception.DuplicateHandlerException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HandlerRegistry")
class HandlerRegistryTest {

  record TestCommand(String value) implements Command<String> {}

  record OtherCommand() implements Command<Void> {}

  static class TestCommandHandler implements CommandHandler<TestCommand, String> {
    @Override
    public String handle(TestCommand cmd) {
      return cmd.value();
    }
  }

  static class DuplicateHandler implements CommandHandler<TestCommand, String> {
    @Override
    public String handle(TestCommand cmd) {
      return "dup";
    }
  }

  static class TestVoidHandler implements VoidCommandHandler<OtherCommand> {
    @Override
    public void handle(OtherCommand cmd) {}
  }

  static class ConflictingVoidHandler implements VoidCommandHandler<TestCommand> {
    @Override
    public void handle(TestCommand cmd) {}
  }

  @Nested
  @DisplayName("buildCommandRegistry")
  class BuildCommandRegistry {

    @Test
    @DisplayName("should register command and void handlers by message type")
    void shouldRegisterBothHandlerTypes() {
      var ch = new TestCommandHandler();
      var vh = new TestVoidHandler();

      var registry = HandlerRegistry.buildCommandRegistry(Map.of("ch", ch), Map.of("vh", vh));

      assertThat(registry).hasSize(2);
      assertThat(registry.get(TestCommand.class)).isSameAs(ch);
      assertThat(registry.get(OtherCommand.class)).isSameAs(vh);
    }

    @Test
    @DisplayName("should reject duplicate command handlers for the same message type")
    void shouldRejectDuplicateCommandHandlers() {
      var h1 = new TestCommandHandler();
      var h2 = new DuplicateHandler();

      assertThatThrownBy(
              () -> HandlerRegistry.buildCommandRegistry(Map.of("h1", h1, "h2", h2), Map.of()))
          .isInstanceOf(DuplicateHandlerException.class);
    }

    @Test
    @DisplayName("should reject void handler conflicting with command handler on same message type")
    void shouldRejectVoidConflictingWithCommand() {
      var ch = new TestCommandHandler();
      var vh = new ConflictingVoidHandler();

      assertThatThrownBy(
              () -> HandlerRegistry.buildCommandRegistry(Map.of("ch", ch), Map.of("vh", vh)))
          .isInstanceOf(DuplicateHandlerException.class);
    }

    @Test
    @DisplayName("should return immutable registry")
    void shouldReturnImmutableMap() {
      var registry =
          HandlerRegistry.buildCommandRegistry(Map.of("ch", new TestCommandHandler()), Map.of());

      @SuppressWarnings("unchecked")
      Map<Object, Object> raw = (Map<Object, Object>) (Map<?, ?>) registry;
      assertThatThrownBy(() -> raw.put(Object.class, "x"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should build empty registry when no handlers provided")
    void shouldBuildEmptyRegistryWhenNoHandlers() {
      var registry = HandlerRegistry.buildCommandRegistry(Map.of(), Map.of());
      assertThat(registry).isEmpty();
    }
  }

  @Nested
  @DisplayName("buildRegistry (generic — used by QueryBus)")
  class BuildGenericRegistry {

    @Test
    @DisplayName("should build registry with custom type resolver")
    void shouldBuildWithResolver() {
      var ch = new TestCommandHandler();
      var registry =
          HandlerRegistry.<CommandHandler<?, ?>>buildRegistry(
              Map.of("ch", ch), HandlerTypeResolver::resolveCommandHandlerMessageType, "TestBus");

      assertThat(registry).hasSize(1);
      assertThat(registry.get(TestCommand.class)).isSameAs(ch);
    }
  }
}
