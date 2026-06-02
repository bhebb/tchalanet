package com.tchalanet.server.platform.tenant.internal.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.platform.tenant.api.model.BusinessDayOverrideView;
import com.tchalanet.server.platform.tenant.internal.persistence.BusinessDayOverrideJpaEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface BusinessDayOverrideMapper {

  @Mapping(target = "id", source = "e.id")
  @Mapping(target = "tenantId", source = "e.tenantId")
  BusinessDayOverrideView toView(BusinessDayOverrideJpaEntity e);

  List<BusinessDayOverrideView> toViews(List<BusinessDayOverrideJpaEntity> entities);
}
