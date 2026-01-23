package com.tchalanet.server.catalog.resultslot;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tchalanet.server.catalog.resultslot.internal.ResultSlotCatalogImpl;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.error.NotFoundException;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ResultSlotCatalogTest {

  @Test
  void listActive_delegatesToReader() {
    var reader = Mockito.mock(ResultSlotReaderPort.class);
    var catalog = new ResultSlotCatalogImpl(reader);

    var view = new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        "SLOT1",
        "prov",
        ZoneId.of("UTC"),
        LocalTime.of(12, 0),
        "MON,TUE",
        true,
        JsonNodeFactory.instance.objectNode(),
        JsonNodeFactory.instance.objectNode(),
        "label.key");

    Mockito.when(reader.listActive()).thenReturn(List.of(view));

    var res = catalog.listActive();
    Assertions.assertNotNull(res);
    Assertions.assertEquals(1, res.size());
    Assertions.assertEquals("SLOT1", res.get(0).slotKey());
    Mockito.verify(reader).listActive();
  }

  @Test
  void findByKey_and_requireByKey_behaviour() {
    var reader = Mockito.mock(ResultSlotReaderPort.class);
    var catalog = new ResultSlotCatalogImpl(reader);

    var view = new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        "SLOT_A",
        "prov",
        ZoneId.of("UTC"),
        LocalTime.of(10, 30),
        "MON",
        true,
        JsonNodeFactory.instance.objectNode(),
        JsonNodeFactory.instance.objectNode(),
        null);

    Mockito.when(reader.findBySlotKey("SLOT_A")).thenReturn(Optional.of(view));

    var opt = catalog.findByKey("SLOT_A");
    Assertions.assertTrue(opt.isPresent());
    Assertions.assertEquals("SLOT_A", opt.get().slotKey());

    var req = catalog.requireByKey("SLOT_A");
    Assertions.assertEquals("SLOT_A", req.slotKey());

    // absent case
    Mockito.when(reader.findBySlotKey("MISSING")).thenReturn(Optional.empty());
    var optMissing = catalog.findByKey("MISSING");
    Assertions.assertTrue(optMissing.isEmpty());
    Assertions.assertThrows(NotFoundException.class, () -> catalog.requireByKey("MISSING"));
  }
}
