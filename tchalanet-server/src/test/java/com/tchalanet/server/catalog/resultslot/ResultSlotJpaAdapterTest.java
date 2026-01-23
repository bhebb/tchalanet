package com.tchalanet.server.catalog.resultslot;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotRestRepository;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ResultSlotJpaAdapterTest {

  @Test
  void listActive_maps_entities_to_views_and_calls_repo() {
    var repo = Mockito.mock(ResultSlotRestRepository.class);
    var adapter = new ResultSlotJpaAdapter(repo);

    var e = new ResultSlotJpaEntity();
    e.setId(UUID.randomUUID());
    e.setSlotKey("slot_x");
    e.setProvider("prov");
    e.setTimezone("UTC");
    e.setDrawTime(LocalTime.of(9, 0));
    e.setDaysOfWeek("MON,TUE");
    e.setActive(true);
    e.setSourceCfg(JsonNodeFactory.instance.objectNode());
    e.setProjectionCfg(JsonNodeFactory.instance.objectNode());

    Mockito.when(repo.findByActiveTrue(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(e)));

    var list = adapter.listActive();
    Assertions.assertEquals(1, list.size());
    ResultSlotView v = list.get(0);
    Assertions.assertEquals("slot_x", v.slotKey());
    Assertions.assertEquals(ZoneId.of("UTC"), v.timezone());
  }

  @Test
  void findBySlotKey_returns_empty_on_blank() {
    var repo = Mockito.mock(ResultSlotRestRepository.class);
    var adapter = new ResultSlotJpaAdapter(repo);
    var opt = adapter.findBySlotKey("  ");
    Assertions.assertTrue(opt.isEmpty());
  }

  @Test
  void findById_delegates_to_repo_and_maps() {
    var repo = Mockito.mock(ResultSlotRestRepository.class);
    var adapter = new ResultSlotJpaAdapter(repo);
    var e = new ResultSlotJpaEntity();
    UUID id = UUID.randomUUID();
    e.setId(id);
    e.setSlotKey("SLOTID");
    e.setProvider("prov");
    e.setTimezone("UTC");
    e.setDrawTime(LocalTime.of(8, 30));
    e.setDaysOfWeek("WED");
    e.setSourceCfg(JsonNodeFactory.instance.objectNode());
    e.setProjectionCfg(JsonNodeFactory.instance.objectNode());

    Mockito.when(repo.findById(id)).thenReturn(Optional.of(e));

    var opt = adapter.findById(id);
    Assertions.assertTrue(opt.isPresent());
    Assertions.assertEquals("SLOTID", opt.get().slotKey());
  }
}
