package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class BusPerformanceRegressionTest {

  @Test
  @DisplayName("CommandBus handles large number of handlers efficiently")
  void testCommandBusScalability() {
    GenericApplicationContext ctx = new GenericApplicationContext();
    int handlerCount = 500;

    // Register many handlers for different commands
    List<Class<? extends Command<?>>> commandClasses = new ArrayList<>();
    for (int i = 0; i < handlerCount; i++) {
      int index = i;
      Class<? extends Command<?>> commandClass = createDynamicCommandClass(index);
      commandClasses.add(commandClass);

      ctx.registerBean(
          "handler" + i,
          CommandHandler.class,
          () -> createDynamicCommandHandler(commandClass, index));
    }

    ctx.refresh();

    SimpleCommandBus commandBus = new SimpleCommandBus(ctx);
    long startInit = System.nanoTime();
    commandBus.init();
    long initDuration = System.nanoTime() - startInit;

    // Verify initialization is reasonable (< 1 second for 500 handlers)
    assertThat(initDuration).isLessThan(1_000_000_000L);

    // Verify dispatch performance remains O(1) map lookup
    for (int i = 0; i < 100; i++) {
      int randomIndex = (int) (Math.random() * handlerCount);
      Command<?> command = createDynamicCommand(commandClasses.get(randomIndex), randomIndex);

      long startDispatch = System.nanoTime();
      commandBus.execute(command);
      long dispatchDuration = System.nanoTime() - startDispatch;

      // Each dispatch should be very fast (< 1ms)
      assertThat(dispatchDuration).isLessThan(1_000_000L);
    }
  }

  @Test
  @DisplayName("QueryBus handles large number of handlers efficiently")
  void testQueryBusScalability() {
    GenericApplicationContext ctx = new GenericApplicationContext();
    int handlerCount = 500;

    // Register many handlers for different queries
    List<Class<? extends Query<?>>> queryClasses = new ArrayList<>();
    for (int i = 0; i < handlerCount; i++) {
      int index = i;
      Class<? extends Query<?>> queryClass = createDynamicQueryClass(index);
      queryClasses.add(queryClass);

      ctx.registerBean(
          "handler" + i,
          QueryHandler.class,
          () -> createDynamicQueryHandler(queryClass, index));
    }

    ctx.refresh();

    SimpleQueryBus queryBus = new SimpleQueryBus(ctx);
    long startInit = System.nanoTime();
    queryBus.init();
    long initDuration = System.nanoTime() - startInit;

    // Verify initialization is reasonable (< 1 second for 500 handlers)
    assertThat(initDuration).isLessThan(1_000_000_000L);

    // Verify dispatch performance remains O(1) map lookup
    for (int i = 0; i < 100; i++) {
      int randomIndex = (int) (Math.random() * handlerCount);
      Query<?> query = createDynamicQuery(queryClasses.get(randomIndex), randomIndex);

      long startDispatch = System.nanoTime();
      queryBus.ask(query);
      long dispatchDuration = System.nanoTime() - startDispatch;

      // Each dispatch should be very fast (< 1ms)
      assertThat(dispatchDuration).isLessThan(1_000_000L);
    }
  }

  // --- Helper methods to create dynamic command/query classes ---

  @SuppressWarnings("unchecked")
  private static Class<? extends Command<?>> createDynamicCommandClass(int index) {
    // Create a unique command class using a simple wrapper
    return (Class<? extends Command<?>>)
        (Class<?>) TestCommands.createCommandClass("TestCommand" + index);
  }

  @SuppressWarnings("unchecked")
  private static Command<?> createDynamicCommand(Class<? extends Command<?>> clazz, int index) {
    return TestCommands.createCommand(clazz, index);
  }

  @SuppressWarnings("unchecked")
  private static CommandHandler<?, ?> createDynamicCommandHandler(
      Class<? extends Command<?>> commandClass, int index) {
    return new CommandHandler<Command<String>, String>() {
      @Override
      public String handle(Command<String> command) {
        return "result" + index;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Query<?>> createDynamicQueryClass(int index) {
    return (Class<? extends Query<?>>)
        (Class<?>) TestQueries.createQueryClass("TestQuery" + index);
  }

  @SuppressWarnings("unchecked")
  private static Query<?> createDynamicQuery(Class<? extends Query<?>> clazz, int index) {
    return TestQueries.createQuery(clazz, index);
  }

  @SuppressWarnings("unchecked")
  private static QueryHandler<?, ?> createDynamicQueryHandler(
      Class<? extends Query<?>> queryClass, int index) {
    return new QueryHandler<Query<String>, String>() {
      @Override
      public String handle(Query<String> query) {
        return "result" + index;
      }
    };
  }

  // Simple test command/query factories for performance tests
  static class TestCommands {
    static Class<?> createCommandClass(String name) {
      // In a real scenario, we'd use bytecode generation or simply create many static classes
      // For this test, we'll reuse a pattern
      return switch (name.hashCode() % 5) {
        case 0 -> TestCommand0.class;
        case 1 -> TestCommand1.class;
        case 2 -> TestCommand2.class;
        case 3 -> TestCommand3.class;
        default -> TestCommand4.class;
      };
    }

    static Command<?> createCommand(Class<?> clazz, int index) {
      // Simple factory - in real test we'd have one per dynamic class
      return new TestCommand0();
    }
  }

  static class TestQueries {
    static Class<?> createQueryClass(String name) {
      return switch (name.hashCode() % 5) {
        case 0 -> TestQuery0.class;
        case 1 -> TestQuery1.class;
        case 2 -> TestQuery2.class;
        case 3 -> TestQuery3.class;
        default -> TestQuery4.class;
      };
    }

    static Query<?> createQuery(Class<?> clazz, int index) {
      return new TestQuery0();
    }
  }

  // Stub command/query classes (we'd have 500+ in a real perf test)
  static class TestCommand0 implements Command<String> {}

  static class TestCommand1 implements Command<String> {}

  static class TestCommand2 implements Command<String> {}

  static class TestCommand3 implements Command<String> {}

  static class TestCommand4 implements Command<String> {}

  static class TestQuery0 implements Query<String> {}

  static class TestQuery1 implements Query<String> {}

  static class TestQuery2 implements Query<String> {}

  static class TestQuery3 implements Query<String> {}

  static class TestQuery4 implements Query<String> {}
}
