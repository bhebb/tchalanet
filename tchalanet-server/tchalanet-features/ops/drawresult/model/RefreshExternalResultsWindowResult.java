package com.tchalanet.server.features.ops.drawresult.model;

import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowResult;

public record RefreshExternalResultsWindowResult(
    int fetched, int projectedOk, int projectedFail, int upserted, int applied, int notFound) {

  public static RefreshExternalResultsWindowResult empty() {
    return new RefreshExternalResultsWindowResult(0, 0, 0, 0, 0, 0);
  }

  public static RefreshExternalResultsWindowResult from(
      FetchExternalResultsWindowResult fetch, ApplyExternalResultsWindowResult apply) {
    if (fetch == null && apply == null) return empty();
    int fetched = 0;
    int upserted = 0;
    int applied = 0;
    int notFound = 0;
    int projectedOk = 0;
    int projectedFail = 0;

    if (fetch != null) {
      fetched = fetch.inserted() + fetch.updated() + fetch.skipped();
      upserted = fetch.inserted() + fetch.updated();
      notFound += fetch.notFound();
    }
    if (apply != null) {
      applied = apply.inserted() + apply.updated();
      notFound += apply.notFound();
      projectedFail = apply.errors();
    }

    return new RefreshExternalResultsWindowResult(
        fetched, projectedOk, projectedFail, upserted, applied, notFound);
  }
}
