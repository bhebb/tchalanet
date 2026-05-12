package com.tchalanet.server.catalog.resultslot.internal.mapper;


import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.common.json.mapper.CommonIdMapper;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface ResultSlotMapper {

  @Mapping(target = "timezone", source = "e.timezone")
  ResultSlotView toView(ResultSlotJpaEntity e);

  List<ResultSlotView> toViews(List<ResultSlotJpaEntity> entities);
}
