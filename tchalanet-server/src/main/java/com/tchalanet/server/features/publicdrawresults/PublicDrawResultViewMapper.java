package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotDetailsView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotView;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultView;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultItemResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSlotResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSlotsResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicDrawResultViewMapper {

  public PublicDrawResultSlotsResponse toSlotsResponse(List<PublicDrawResultSlotView> slots) {
    return new PublicDrawResultSlotsResponse(
        slots.stream()
            .map(
                slot ->
                    new PublicDrawResultSlotResponse(
                        slot.slotKey(),
                        slot.provider(),
                        slot.label(),
                        slot.timezone(),
                        slot.drawTime(),
                        slot.next(),
                        toItem(slot, slot.latest()),
                        List.of()))
            .toList());
  }

  public PublicDrawResultSlotsResponse toDetailsResponse(
      List<PublicDrawResultSlotDetailsView> slots) {
    return new PublicDrawResultSlotsResponse(
        slots.stream()
            .map(
                slot ->
                    new PublicDrawResultSlotResponse(
                        slot.slotKey(),
                        slot.provider(),
                        slot.label(),
                        slot.timezone(),
                        slot.drawTime(),
                        slot.next(),
                        toItem(slot, slot.latest()),
                        slot.history().stream().map(this::toItem).toList()))
            .toList());
  }

  public PublicDrawResultListResponse toHistoryResponse(TchPage<PublicDrawResultHistoryRowView> page) {
    return new PublicDrawResultListResponse(
        page.items().stream().map(this::toItem).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  private PublicDrawResultItemResponse toItem(
      PublicDrawResultSlotView slot, PublicDrawResultView result) {
    if (result == null) {
      return null;
    }

    return new PublicDrawResultItemResponse(
        slot.slotKey(),
        slot.provider(),
        slot.label(),
        slot.timezone(),
        slot.drawTime(),
        result.resultDate(),
        result.occurredAt(),
        result.status(),
        result.quality(),
        result.haiti(),
        result.source());
  }

  private PublicDrawResultItemResponse toItem(
      PublicDrawResultSlotDetailsView slot, PublicDrawResultView result) {
    if (result == null) {
      return null;
    }

    return new PublicDrawResultItemResponse(
        slot.slotKey(),
        slot.provider(),
        slot.label(),
        slot.timezone(),
        slot.drawTime(),
        result.resultDate(),
        result.occurredAt(),
        result.status(),
        result.quality(),
        result.haiti(),
        result.source());
  }

  private PublicDrawResultItemResponse toItem(PublicDrawResultHistoryRowView row) {
    return new PublicDrawResultItemResponse(
        row.slotKey(),
        row.provider(),
        row.label(),
        row.timezone(),
        row.drawTime(),
        row.resultDate(),
        row.occurredAt(),
        row.status(),
        row.quality(),
        row.haiti(),
        row.source());
  }
}
