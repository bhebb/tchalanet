package com.tchalanet.server.common.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.exception.NoHandlerException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleQueryBus")
class SimpleQueryBusTest {

    record GetNameQuery(String id) implements Query<String> {}
    record UnregisteredQuery() implements Query<Integer> {}

    static class GetNameHandler implements QueryHandler<GetNameQuery, String> {
        @Override public String handle(GetNameQuery q) { return "name:" + q.id(); }
    }

    @SuppressWarnings("unchecked")
    private static SimpleQueryBus busWithHandlers(Map<Class<?>, QueryHandler<?, ?>> handlers) {
        var bus = new SimpleQueryBus(null);
        try {
            var field = SimpleQueryBus.class.getDeclaredField("handlers");
            field.setAccessible(true);
            field.set(bus, handlers);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return bus;
    }

    @Nested
    @DisplayName("ask")
    class Ask {

        @Test
        @DisplayName("should dispatch to QueryHandler and return result")
        void shouldDispatchQueryHandler() {
            var handler = new GetNameHandler();
            var bus = busWithHandlers(Map.of(GetNameQuery.class, handler));

            String result = bus.ask(new GetNameQuery("42"));

            assertThat(result).isEqualTo("name:42");
        }

        @Test
        @DisplayName("should throw NullPointerException for null query")
        void shouldRejectNullQuery() {
            var bus = busWithHandlers(Map.of(GetNameQuery.class, new GetNameHandler()));

            assertThatThrownBy(() -> bus.ask(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException when registry is empty")
        void shouldRejectWhenNotInitialized() {
            var bus = new SimpleQueryBus(null);

            assertThatThrownBy(() -> bus.ask(new GetNameQuery("x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
        }

        @Test
        @DisplayName("should throw NoHandlerException for unregistered query")
        void shouldThrowNoHandlerForUnknownQuery() {
            var bus = busWithHandlers(Map.of(GetNameQuery.class, new GetNameHandler()));

            assertThatThrownBy(() -> bus.ask(new UnregisteredQuery()))
                .isInstanceOf(NoHandlerException.class);
        }
    }
}
