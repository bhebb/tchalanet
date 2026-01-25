package com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface PageModelTemplateMapper {

  PageModelTemplateView toView(PageModelTemplateEntity e);

  List<PageModelTemplateView> toViews(List<PageModelTemplateEntity> entities);
}
