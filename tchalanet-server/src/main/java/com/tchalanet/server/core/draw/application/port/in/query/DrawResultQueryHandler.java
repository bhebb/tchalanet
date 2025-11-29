package com.tchalanet.server.core.draw.application.port.in.query;

import com.tchalanet.server.core.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawResultQuery;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.util.List;

public interface DrawResultQueryHandler {
  DrawResult get(GetDrawResultQuery query);

  List<DrawResult> list(ListDrawResultsQuery query);

  List<DrawResult> listToday(ListTodayDrawResultQuery query);

  List<DrawResult> listLastDays(ListLastDaysDrawResultsQuery query);
}
