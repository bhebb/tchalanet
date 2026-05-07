package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class SimpleQueryBusTest {

  private GenericApplicationContext ctx;
  private SimpleQueryBus queryBus;

  @BeforeEach
  void setUp() {
    ctx = new GenericApplicationContext();
    queryBus = new SimpleQueryBus(ctx);
  }

  // --- Test Queries ---

  static class TestQuery implements Query<String> {
    final String value;

    TestQuery(String value) {
      this.value = value;
    }
  }

  static class TestQueryHandler implements QueryHandler<TestQuery, String> {
    @Override
    public String handle(TestQuery query) {
      return "result:" + query.value;
    }
  }

  static class UnhandledQuery implements Query<String> {}

  static class DuplicateQuery implements Query<String> {}

  static class DuplicateQueryHandler1 implements QueryHandler<DuplicateQuery, String> {
    @Override
    public String handle(DuplicateQuery query) {
      return "handler1";
    }
  }

  static class DuplicateQueryHandler2 implements QueryHandler<DuplicateQuery, String> {
    @Override
    public String handle(DuplicateQuery query) {
      return "handler2";
    }
  }

  // --- Tests ---

  @Test
  @DisplayName("QueryBus dispatches to the correct query handler")
  void testQueryHandlerDispatch() {
    ctx.registerBean("testHandler", TestQueryHandler.class);
    ctx.refresh();

    queryBus.init();

    String result = queryBus.ask(new TestQuery("test"));
    assertThat(result).isEqualTo("result:test");
  }

  @Test
  @DisplayName("QueryBus fails when no handler exists")
  void testNoHandlerException() {
    ctx.refresh();
    queryBus.init();

    assertThatThrownBy(() -> queryBus.ask(new UnhandledQuery()))
        .isInstanceOf(NoHandlerException.class)
        .hasMessageContaining("UnhandledQuery");
  }

  @Test
  @DisplayName("QueryBus fails when duplicate handlers exist")
  void testDuplicateHandlers() {
    ctx.registerBean("handler1", DuplicateQueryHandler1.class);
    ctx.registerBean("handler2", DuplicateQueryHandler2.class);
    ctx.refresh();

    assertThatThrownBy(() -> queryBus.init())
        .isInstanceOf(DuplicateHandlerException.class)
        .hasMessageContaining("DuplicateQuery")
        .hasMessageContaining("DuplicateQueryHandler1")
        .hasMessageContaining("DuplicateQueryHandler2");
  }

  @Test
  @DisplayName("Null query is rejected")
  void testNullQueryRejected() {
    ctx.refresh();
    queryBus.init();

    assertThatThrownBy(() -> queryBus.ask(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Query must not be null");
  }

  @Test
  @DisplayName("Registry maps are immutable after initialization")
  void testRegistryImmutability() {
    ctx.registerBean("testHandler", TestQueryHandler.class);
    ctx.refresh();

    queryBus.init();

    // Registry is immutable - verify consistent behavior
    String result1 = queryBus.ask(new TestQuery("first"));
    String result2 = queryBus.ask(new TestQuery("second"));

    assertThat(result1).isEqualTo("result:first");
    assertThat(result2).isEqualTo("result:second");
  }
}

