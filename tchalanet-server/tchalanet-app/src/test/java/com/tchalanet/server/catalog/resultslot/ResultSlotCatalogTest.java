package com.tchalanet.server.catalog.resultslot;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@DisplayName("ResultSlotCatalog serialization")
class ResultSlotCatalogTest {

  private static final GenericJacksonJsonRedisSerializer SERIALIZER =
	  GenericJacksonJsonRedisSerializer.builder()
		  .customize(
			  builder ->
				  builder.activateDefaultTypingAsProperty(
					  BasicPolymorphicTypeValidator.builder()
						  .allowIfSubType("com.tchalanet.server.")
						  .allowIfSubType("java.time.")
						  .allowIfSubType("java.util.")
						  .allowIfSubType("tools.jackson.")
						  .build(),
					  DefaultTyping.NON_FINAL_AND_RECORDS,
					  "@class"))
		  .build();

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Nested
  @DisplayName("When serializing result slot views")
  class WhenSerializingResultSlotViews {

	@Test
	@DisplayName("should deserialize a single view as ResultSlotView")
	void shouldDeserializeSingleViewAsResultSlotView() {
	  var source = sampleView();

	  var roundTrip = SERIALIZER.deserialize(SERIALIZER.serialize(source));

	  assertThat(roundTrip).isInstanceOf(ResultSlotView.class);
	  assertThat(roundTrip).isNotInstanceOf(java.util.Map.class);

	  var typed = (ResultSlotView) roundTrip;
	  assertThat(typed.sourceCfg()).isNotNull();
	  assertThat(typed.sourceCfg().isObject()).isTrue();
	  assertThat(typed.sourceCfg().path("provider").asText()).isEqualTo("test");
	  assertThat(typed.projectionCfg()).isNotNull();
	  assertThat(typed.projectionCfg().isObject()).isTrue();
	  assertThat(typed.projectionCfg().path("kind").asText()).isEqualTo("test");
	}

	private ResultSlotView sampleView() {
	  JsonNode sourceCfg = JSON_MAPPER.createObjectNode().put("provider", "test");
	  JsonNode projectionCfg = JSON_MAPPER.createObjectNode().put("kind", "test");
	  return new ResultSlotView(
		  ResultSlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
		  "GA_EVE",
		  "GA",
		  ZoneId.of("America/New_York"),
		  LocalTime.of(18, 0),
		  "MON,TUE",
		  true,
		  sourceCfg,
		  projectionCfg,
		  "result.slot.ga.eve");
	}
  }
}
