package com.tchalanet.server.catalog.drawchannel.internal.mapper;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelGameEntity;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.DrawChannelGameResponse;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface DrawChannelGameMapper {

  @Mapping(source = "drawChannelId", target = "drawChannelId")
  @Mapping(source = "tenantGameId", target = "tenantGameId")
  DrawChannelGameView toView(DrawChannelGameEntity e);

  List<DrawChannelGameView> toViews(List<DrawChannelGameEntity> list);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "drawChannelId", target = "drawChannelId")
  @Mapping(source = "tenantGameId", target = "tenantGameId")
  DrawChannelGameResponse toResponse(DrawChannelGameEntity e);

  List<DrawChannelGameResponse> toResponses(List<DrawChannelGameEntity> list);
}
