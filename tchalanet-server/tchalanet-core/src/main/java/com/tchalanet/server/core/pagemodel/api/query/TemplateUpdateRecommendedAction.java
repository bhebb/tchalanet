package com.tchalanet.server.core.pagemodel.api.query;

public enum TemplateUpdateRecommendedAction {
  MERGE_SAFE,
  MERGE_WITH_CONFLICTS,
  CREATE_DRAFT,
  REPLACE_ALL,
  IGNORE,
  REQUIRES_MIGRATION
}
