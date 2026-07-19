package com.tchalanet.server.platform.publiccontent.api.model;

/** Explicit outcome of an external-news refresh; stale content remains safe to serve. */
public record PublicContentRefreshResult(Status status, int itemCount) {

  public enum Status {
    REFRESHED,
    STALE_CACHE_RETAINED
  }

  public static PublicContentRefreshResult refreshed(int itemCount) {
    return new PublicContentRefreshResult(Status.REFRESHED, itemCount);
  }

  public static PublicContentRefreshResult staleCacheRetained(int itemCount) {
    return new PublicContentRefreshResult(Status.STALE_CACHE_RETAINED, itemCount);
  }
}
