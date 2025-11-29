package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.infra.web.model.DrawResultResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultWebMapper {
  DrawResultResponse toResponse(DrawResult drawResult);

  DrawResult toDomain(DrawResultResponse response);
}
