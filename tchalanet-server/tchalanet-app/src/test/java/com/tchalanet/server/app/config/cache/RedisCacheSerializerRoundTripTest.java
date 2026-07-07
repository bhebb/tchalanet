package com.tchalanet.server.app.config.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.util.List;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Round-trip test for the L2 (Redis) cache value serializer, exercised without a live Redis.
 *
 * <p>Reproduces the two production failures observed on {@code catalog:resultslot:v2:active} and
 * {@code platform.tenant.cache.REGISTRY_BY_ID}: a value carrying typed-id wrappers ({@link
 * ResultSlotId}) plus {@code JsonNode} config fields, and a {@code List<...>} cache value whose root
 * (an immutable {@code java.util.ImmutableCollections$*}) must remain type-tagged to be readable.
 */
class RedisCacheSerializerRoundTripTest {

  @SuppressWarnings("unchecked")
  private final RedisSerializer<Object> serializer =
      (RedisSerializer<Object>) RedisCacheRuntimeConfig.cacheValueSerializer();

  @Test
  void resultSlotView_withTypedId_and_jsonNode_roundTrips() {
    JsonMapper jm = JsonMapper.builder().build();
    JsonNode cfg = jm.readTree("{\"src\":\"OH\",\"weight\":3,\"nested\":{\"a\":true}}");

    ResultSlotView original =
        new ResultSlotView(
            ResultSlotId.of(UUID.randomUUID()),
            "borlette-am",
            "OH",
            ZoneId.of("America/Port-au-Prince"),
            LocalTime.of(20, 0),
            "MON,TUE,WED",
            true,
            cfg,
            cfg,
            "label.borlette.am");

    byte[] bytes = serializer.serialize(original);
    assertNotNull(bytes);

    Object back = serializer.deserialize(bytes);
    assertInstanceOf(ResultSlotView.class, back);
    assertEquals(original, back);
  }

  @Test
  void listOfResultSlotView_roundTrips() {
    // The production chain was ArrayList[0] -> ResultSlotView[id]; a cached List<View> is the shape.
    // NOTE: JsonNode fields must be non-null here — a Java-null JsonNode round-trips back as
    // NullNode (not null), so the record would not be equal. That null->NullNode asymmetry is a
    // real caveat for caching JsonNode-bearing views (tracked separately).
    JsonNode cfg = JsonMapper.builder().build().readTree("{\"src\":\"OH\"}");
    ResultSlotView v =
        new ResultSlotView(
            ResultSlotId.of(UUID.randomUUID()),
            "k",
            "OH",
            ZoneId.of("America/Port-au-Prince"),
            LocalTime.NOON,
            "SUN",
            false,
            cfg,
            cfg,
            "label");

    Object back = serializer.deserialize(serializer.serialize(List.of(v)));

    assertInstanceOf(List.class, back);
    List<?> list = (List<?>) back;
    assertEquals(1, list.size());
    assertEquals(v, list.get(0));
  }

  @Test
  void nullValue_roundTrips() {
    byte[] bytes = serializer.serialize(NullValue.INSTANCE);
    assertNotNull(bytes);
    assertInstanceOf(NullValue.class, serializer.deserialize(bytes));
  }

  @Test
  void immutableList_roundTrips() {
    // Stream.toList()/List.of()/List.copyOf() return final ImmutableCollections types; they must
    // stay type-tagged at the JSON root to be readable back.
    List<String> original = List.of("a", "b", "c");
    Object back = serializer.deserialize(serializer.serialize(original));
    assertEquals(original, back);
  }
}
