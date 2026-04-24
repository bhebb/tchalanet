package com.tchalanet.server.core.pagemodel.application.port;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.List;

public interface PageModelWritePort {
  PageModelInstance save(PageModelInstance instance);
  List<PageModelInstance> saveAll(List<PageModelInstance> instances);
}

