package com.tchalanet.server.catalog.resultslot.internal.mapper;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotJpaEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ResultSlotMapper {

  ResultSlotView toView(ResultSlotJpaEntity e);

  List<ResultSlotView> toViews(List<ResultSlotJpaEntity> entities);
}
