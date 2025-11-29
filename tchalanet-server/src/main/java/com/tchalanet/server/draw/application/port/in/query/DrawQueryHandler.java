package com.tchalanet.server.draw.application.port.in.query;

import com.tchalanet.server.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.draw.application.query.model.ListLastDaysDrawsQuery;
import com.tchalanet.server.draw.application.query.model.ListTodayDrawsQuery;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawSummary;
import java.util.List;

/** Query handler port for draw queries. */
public interface DrawQueryHandler {

  Draw get(GetDrawQuery query);

  List<DrawSummary> list(ListDrawsQuery query);

  List<DrawSummary> listToday(ListTodayDrawsQuery query);

  List<DrawSummary> listLastDays(ListLastDaysDrawsQuery query);

  Draw getNext(GetNextDrawQuery query);

  List<Draw> getNextForChannels(GetNextDrawsQuery query);
}
