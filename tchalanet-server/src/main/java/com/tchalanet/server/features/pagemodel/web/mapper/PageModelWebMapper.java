package com.tchalanet.server.features.pagemodel.web.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.features.pagemodel.domain.model.PageModel;
import com.tchalanet.server.features.pagemodel.web.dto.PublicHomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageModelWebMapper {

  private final ObjectMapper objectMapper;

  public PublicHomeResponse toPublicHomeResponse(PageModel pageModel) {
    JsonNode node;
    try {
      node = objectMapper.readTree(pageModel.getJson());
    } catch (Exception e) {
      throw new IllegalStateException("Invalid PageModel JSON for id=" + pageModel.getId(), e);
    }
    return new PublicHomeResponse(pageModel.getCode(), pageModel.getLang(), node);
  }
}
