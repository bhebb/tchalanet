package com.tchalanet.server.draw.infra.web.mapper;

import com.tchalanet.server.draw.domain.model.DrawResult;
import com.tchalanet.server.draw.infra.web.model.DrawResultResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultWebMapper {
  DrawResultResponse toResponse(DrawResult drawResult);

  DrawResult toDomain(DrawResultResponse response);
}
