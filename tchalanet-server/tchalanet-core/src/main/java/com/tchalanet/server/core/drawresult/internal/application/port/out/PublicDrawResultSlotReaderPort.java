package com.tchalanet.server.core.drawresult.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultDetailView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotDetailsView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface PublicDrawResultSlotReaderPort {

  List<PublicDrawResultSlotView> listPublicSlots(List<String> slotKeys, String provider);

  List<PublicDrawResultSlotDetailsView> listPublicSlotDetails(
      List<String> slotKeys, String provider, LocalDate resultDate, int historyLimit);

  TchPage<PublicDrawResultHistoryRowView> searchPublicHistory(
      List<String> slotKeys,
      String provider,
      LocalDate from,
      LocalDate to,
      Pageable pageable);

  Optional<PublicDrawResultDetailView> findPublicResultDetailById(DrawResultId id);
}
