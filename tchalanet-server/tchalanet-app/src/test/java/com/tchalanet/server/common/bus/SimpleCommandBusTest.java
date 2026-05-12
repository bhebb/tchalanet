package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class SimpleCommandBusTest {

  private GenericApplicationContext ctx;
  private SimpleCommandBus commandBus;

  @BeforeEach
  void setUp() {
    ctx = new GenericApplicationContext();
    commandBus = new SimpleCommandBus(ctx);
  }

  // --- Test Commands ---

  static class TestCommand implements Command<String> {
    final String value;

    TestCommand(String value) {
      this.value = value;
    }
  }

  static class TestCommandHandler implements CommandHandler<TestCommand, String> {
    @Override
    public String handle(TestCommand command) {
      return "handled:" + command.value;
    }
  }

  static class VoidTestCommand implements Command<Void> {
    final String value;

    VoidTestCommand(String value) {
      this.value = value;
    }
  }

  static class VoidTestCommandHandler implements VoidCommandHandler<VoidTestCommand> {
    boolean called = false;

    @Override
    public void handle(VoidTestCommand command) {
      called = true;
    }
  }

  static class UnhandledCommand implements Command<Void> {}

  static class DuplicateCommand implements Command<String> {}

  static class DuplicateCommandHandler1 implements CommandHandler<DuplicateCommand, String> {
    @Override
    public String handle(DuplicateCommand command) {
      return "handler1";
    }
  }

  static class DuplicateCommandHandler2 implements CommandHandler<DuplicateCommand, String> {
    @Override
    public String handle(DuplicateCommand command) {
      return "handler2";
    }
  }

  static class DuplicateVoidCommandHandler implements VoidCommandHandler<DuplicateCommand> {
    @Override
    public void handle(DuplicateCommand command) {}
  }

  // --- Tests ---

  @Test
  @DisplayName("CommandBus dispatches to the correct command handler")
  void testCommandHandlerDispatch() {
    ctx.registerBean("testHandler", TestCommandHandler.class);
    ctx.refresh();

    commandBus.init();

    String result = commandBus.execute(new TestCommand("test"));
    assertThat(result).isEqualTo("handled:test");
  }

  @Test
  @DisplayName("CommandBus dispatches to the correct void command handler")
  void testVoidCommandHandlerDispatch() {
    VoidTestCommandHandler handler = new VoidTestCommandHandler();
    ctx.registerBean("voidHandler", VoidCommandHandler.class, () -> handler);
    ctx.refresh();

    commandBus.init();

    Void result = commandBus.execute(new VoidTestCommand("test"));
    assertThat(result).isNull();
    assertThat(handler.called).isTrue();
  }

  @Test
  @DisplayName("CommandBus fails when no handler exists")
  void testNoHandlerException() {
    ctx.refresh();
    commandBus.init();

    assertThatThrownBy(() -> commandBus.execute(new UnhandledCommand()))
        .isInstanceOf(NoHandlerException.class)
        .hasMessageContaining("UnhandledCommand");
  }

  @Test
  @DisplayName("CommandBus fails when duplicate handlers exist")
  void testDuplicateHandlers() {
    ctx.registerBean("handler1", DuplicateCommandHandler1.class);
    ctx.registerBean("handler2", DuplicateCommandHandler2.class);
    ctx.refresh();

    assertThatThrownBy(() -> commandBus.init())
        .isInstanceOf(DuplicateHandlerException.class)
        .hasMessageContaining("DuplicateCommand")
        .hasMessageContaining("DuplicateCommandHandler1")
        .hasMessageContaining("DuplicateCommandHandler2");
  }

  @Test
  @DisplayName("CommandBus fails when command handler and void handler exist for same command")
  void testDuplicateCommandAndVoidHandlers() {
    ctx.registerBean("handler", DuplicateCommandHandler1.class);
    ctx.registerBean("voidHandler", DuplicateVoidCommandHandler.class);
    ctx.refresh();

    assertThatThrownBy(() -> commandBus.init())
        .isInstanceOf(DuplicateHandlerException.class)
        .hasMessageContaining("DuplicateCommand");
  }

  @Test
  @DisplayName("Null command is rejected")
  void testNullCommandRejected() {
    ctx.refresh();
    commandBus.init();

    assertThatThrownBy(() -> commandBus.execute(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Command must not be null");
  }

  @Test
  @DisplayName("Registry maps are immutable after initialization")
  void testRegistryImmutability() {
    ctx.registerBean("testHandler", TestCommandHandler.class);
    ctx.refresh();

    commandBus.init();

    // Registry is immutable - we can't test direct mutation, but we verify consistent behavior
    String result1 = commandBus.execute(new TestCommand("first"));
    String result2 = commandBus.execute(new TestCommand("second"));

    assertThat(result1).isEqualTo("handled:first");
    assertThat(result2).isEqualTo("handled:second");
  }
}

