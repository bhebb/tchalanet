package com.tchalanet.server.common.usecase;

import java.util.List;
import java.util.Map;

public interface ListPublicNewsUseCase {
  List<Map<String, Object>> listPublicNews();
}
