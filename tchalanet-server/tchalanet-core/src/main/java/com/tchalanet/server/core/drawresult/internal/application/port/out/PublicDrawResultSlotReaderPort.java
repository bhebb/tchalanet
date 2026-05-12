package com.tchalanet.server.core.drawresult.internal.application.port.out;

import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotDetailsView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PublicDrawResultSlotReaderPort {

  List<PublicDrawResultSlotView> listPublicSlots(List<String> slotKeys, String provider);

  List<PublicDrawResultSlotDetailsView> listPublicSlotDetails(
      List<String> slotKeys, String provider, int historyLimit);

  TchPage<PublicDrawResultHistoryRowView> searchPublicHistory(
      List<String> slotKeys,
      String provider,
      LocalDate from,
      LocalDate to,
      Pageable pageable);
}
