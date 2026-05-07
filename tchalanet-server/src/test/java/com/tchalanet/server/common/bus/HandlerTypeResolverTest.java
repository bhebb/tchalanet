package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HandlerTypeResolverTest {

  // --- Direct handler implementations ---

  static class DirectCommand implements Command<String> {}

  static class DirectCommandHandler implements CommandHandler<DirectCommand, String> {
    @Override
    public String handle(DirectCommand command) {
      return "direct";
    }
  }

  static class DirectVoidCommand implements Command<Void> {}

  static class DirectVoidCommandHandler implements VoidCommandHandler<DirectVoidCommand> {
    @Override
    public void handle(DirectVoidCommand command) {}
  }

  static class DirectQuery implements Query<String> {}

  static class DirectQueryHandler implements QueryHandler<DirectQuery, String> {
    @Override
    public String handle(DirectQuery query) {
      return "direct";
    }
  }

  // --- Inherited handler implementations ---

  static abstract class BaseCommandHandler<C extends Command<R>, R>
      implements CommandHandler<C, R> {}

  static class InheritedCommand implements Command<String> {}

  static class InheritedCommandHandler extends BaseCommandHandler<InheritedCommand, String> {
    @Override
    public String handle(InheritedCommand command) {
      return "inherited";
    }
  }

  static abstract class BaseVoidCommandHandler<C extends Command<Void>>
      implements VoidCommandHandler<C> {}

  static class InheritedVoidCommand implements Command<Void> {}

  static class InheritedVoidCommandHandler extends BaseVoidCommandHandler<InheritedVoidCommand> {
    @Override
    public void handle(InheritedVoidCommand command) {}
  }

  static abstract class BaseQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {}

  static class InheritedQuery implements Query<String> {}

  static class InheritedQueryHandler extends BaseQueryHandler<InheritedQuery, String> {
    @Override
    public String handle(InheritedQuery query) {
      return "inherited";
    }
  }

  // --- Raw type handlers (should fail) ---

  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawCommandHandler implements CommandHandler {
    @Override
    public Object handle(Object command) {
      return null;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static class RawQueryHandler implements QueryHandler {
    @Override
    public Object handle(Object query) {
      return null;
    }
  }

  // --- Tests ---

  @Test
  @DisplayName("Resolves direct CommandHandler message type")
  void testDirectCommandHandler() {
    DirectCommandHandler handler = new DirectCommandHandler();
    Class<?> messageType = HandlerTypeResolver.resolveCommandHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(DirectCommand.class);
  }

  @Test
  @DisplayName("Resolves direct VoidCommandHandler message type")
  void testDirectVoidCommandHandler() {
    DirectVoidCommandHandler handler = new DirectVoidCommandHandler();
    Class<?> messageType = HandlerTypeResolver.resolveVoidCommandHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(DirectVoidCommand.class);
  }

  @Test
  @DisplayName("Resolves direct QueryHandler message type")
  void testDirectQueryHandler() {
    DirectQueryHandler handler = new DirectQueryHandler();
    Class<?> messageType = HandlerTypeResolver.resolveQueryHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(DirectQuery.class);
  }

  @Test
  @DisplayName("Resolves inherited CommandHandler message type")
  void testInheritedCommandHandler() {
    InheritedCommandHandler handler = new InheritedCommandHandler();
    Class<?> messageType = HandlerTypeResolver.resolveCommandHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(InheritedCommand.class);
  }

  @Test
  @DisplayName("Resolves inherited VoidCommandHandler message type")
  void testInheritedVoidCommandHandler() {
    InheritedVoidCommandHandler handler = new InheritedVoidCommandHandler();
    Class<?> messageType = HandlerTypeResolver.resolveVoidCommandHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(InheritedVoidCommand.class);
  }

  @Test
  @DisplayName("Resolves inherited QueryHandler message type")
  void testInheritedQueryHandler() {
    InheritedQueryHandler handler = new InheritedQueryHandler();
    Class<?> messageType = HandlerTypeResolver.resolveQueryHandlerMessageType(handler);
    assertThat(messageType).isEqualTo(InheritedQuery.class);
  }

  @Test
  @DisplayName("Fails when CommandHandler generic type cannot be resolved")
  void testUnresolvableCommandHandler() {
    RawCommandHandler handler = new RawCommandHandler();
    assertThatThrownBy(() -> HandlerTypeResolver.resolveCommandHandlerMessageType(handler))
        .isInstanceOf(InvalidHandlerException.class)
        .hasMessageContaining("Cannot resolve message type")
        .hasMessageContaining("RawCommandHandler");
  }

  @Test
  @DisplayName("Fails when QueryHandler generic type cannot be resolved")
  void testUnresolvableQueryHandler() {
    RawQueryHandler handler = new RawQueryHandler();
    assertThatThrownBy(() -> HandlerTypeResolver.resolveQueryHandlerMessageType(handler))
        .isInstanceOf(InvalidHandlerException.class)
        .hasMessageContaining("Cannot resolve message type")
        .hasMessageContaining("RawQueryHandler");
  }
}

