package com.tchalanet.server.catalog.resultslot.internal.mapper;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotCalendarOverrideJpaEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface ResultSlotCalendarOverrideMapper {

  @Mapping(target = "id", source = "e.id")
  @Mapping(target = "resultSlotId", source = "e.resultSlotId")
  ResultSlotCalendarOverrideView toView(ResultSlotCalendarOverrideJpaEntity e);

  List<ResultSlotCalendarOverrideView> toViews(List<ResultSlotCalendarOverrideJpaEntity> entities);
}
