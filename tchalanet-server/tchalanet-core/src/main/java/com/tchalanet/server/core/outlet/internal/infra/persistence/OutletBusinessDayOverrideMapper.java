package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.outlet.api.query.OutletBusinessDayOverrideView;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface OutletBusinessDayOverrideMapper {

  @Mapping(target = "id", source = "e.id")
  @Mapping(target = "tenantId", source = "e.tenantId")
  @Mapping(target = "outletId", source = "e.outletId")
  OutletBusinessDayOverrideView toView(OutletBusinessDayOverrideJpaEntity e);

  List<OutletBusinessDayOverrideView> toViews(List<OutletBusinessDayOverrideJpaEntity> entities);
}
