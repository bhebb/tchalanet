package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.exception.NoHandlerException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleCommandBus")
class SimpleCommandBusTest {

  record StringCommand(String value) implements Command<String> {}

  record VoidCommand() implements Command<Void> {}

  record UnregisteredCommand() implements Command<String> {}

  static class StringHandler implements CommandHandler<StringCommand, String> {
    @Override
    public String handle(StringCommand cmd) {
      return "handled:" + cmd.value();
    }
  }

  static class VoidHandler implements VoidCommandHandler<VoidCommand> {
    boolean called;

    @Override
    public void handle(VoidCommand cmd) {
      called = true;
    }
  }

  private static SimpleCommandBus busWithHandlers(Map<Class<?>, Object> handlers) {
    var bus = new SimpleCommandBus(null);
    try {
      var field = SimpleCommandBus.class.getDeclaredField("handlers");
      field.setAccessible(true);
      field.set(bus, handlers);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return bus;
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should dispatch to CommandHandler and return result")
    void shouldDispatchCommandHandler() {
      var handler = new StringHandler();
      var bus = busWithHandlers(Map.of(StringCommand.class, handler));

      String result = bus.execute(new StringCommand("abc"));

      assertThat(result).isEqualTo("handled:abc");
    }

    @Test
    @DisplayName("should dispatch to VoidCommandHandler and return null")
    void shouldDispatchVoidHandler() {
      var handler = new VoidHandler();
      var bus = busWithHandlers(Map.of(VoidCommand.class, handler));

      Void result = bus.execute(new VoidCommand());

      assertThat(result).isNull();
      assertThat(handler.called).isTrue();
    }

    @Test
    @DisplayName("should throw NullPointerException for null command")
    void shouldRejectNullCommand() {
      var bus = busWithHandlers(Map.of(StringCommand.class, new StringHandler()));

      assertThatThrownBy(() -> bus.execute(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("null");
    }

    @Test
    @DisplayName("should throw IllegalStateException when registry is not initialized")
    void shouldRejectWhenRegistryNotInitialized() {
      var bus = new SimpleCommandBus(null);

      assertThatThrownBy(() -> bus.execute(new StringCommand("x")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not initialized");
    }

    @Test
    @DisplayName("should throw NoHandlerException for unregistered command")
    void shouldThrowNoHandlerForUnknownCommand() {
      var bus = busWithHandlers(Map.of(StringCommand.class, new StringHandler()));

      assertThatThrownBy(() -> bus.execute(new UnregisteredCommand()))
          .isInstanceOf(NoHandlerException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException for invalid handler type")
    void shouldThrowForInvalidHandlerType() {
      var bus = busWithHandlers(Map.of(StringCommand.class, "not-a-handler"));

      assertThatThrownBy(() -> bus.execute(new StringCommand("x")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Invalid handler type");
    }
  }
}
