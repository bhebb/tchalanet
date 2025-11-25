package com.tchalanet.server.common.usecase;

import java.util.List;
import java.util.Map;

public interface NewsProviderPort {
  List<Map<String, Object>> fetchLatest();
}
