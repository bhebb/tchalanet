package com.tchalanet.server.platform.publiccontent.internal.news.provider;

import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentItem;
import java.util.List;

/** Contract for external news feed providers. */
public interface NewsProvider {
  List<PublicContentItem> fetchLatestNews();
}
