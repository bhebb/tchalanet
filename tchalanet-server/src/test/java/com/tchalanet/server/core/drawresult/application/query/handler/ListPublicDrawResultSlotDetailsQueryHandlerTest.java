package com.tchalanet.server.core.drawresult.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotDetailsQuery;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotDetailsView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ListPublicDrawResultSlotDetailsQueryHandlerTest {

  @Test
  void defaultsAndCapsHistoryLimit() {
    var reader = new RecordingReader();
    var handler = new ListPublicDrawResultSlotDetailsQueryHandler(reader);

    handler.handle(new ListPublicDrawResultSlotDetailsQuery(List.of("ny_mid", "NY_MID"), "ny", 0));
    assertThat(reader.historyLimit).isEqualTo(5);
    assertThat(reader.slotKeys).containsExactly("NY_MID");
    assertThat(reader.provider).isEqualTo("NY");

    handler.handle(new ListPublicDrawResultSlotDetailsQuery(List.of(), null, 99));
    assertThat(reader.historyLimit).isEqualTo(10);
  }

  private static final class RecordingReader implements PublicDrawResultSlotReaderPort {
    private List<String> slotKeys;
    private String provider;
    private int historyLimit;

    @Override
    public List<PublicDrawResultSlotView> listPublicSlots(List<String> slotKeys, String provider) {
      return List.of();
    }

    @Override
    public List<PublicDrawResultSlotDetailsView> listPublicSlotDetails(
        List<String> slotKeys, String provider, int historyLimit) {
      this.slotKeys = slotKeys;
      this.provider = provider;
      this.historyLimit = historyLimit;
      return List.of();
    }

    @Override
    public TchPage<PublicDrawResultHistoryRowView> searchPublicHistory(
        List<String> slotKeys,
        String provider,
        LocalDate from,
        LocalDate to,
        Pageable pageable) {
      throw new UnsupportedOperationException();
    }
  }
}
