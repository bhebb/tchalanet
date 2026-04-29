package com.tchalanet.server.core.pagemodel.application.query.model;

public enum TemplateUpdateRecommendedAction {
  MERGE_SAFE,
  MERGE_WITH_CONFLICTS,
  CREATE_DRAFT,
  REPLACE_ALL,
  IGNORE,
  REQUIRES_MIGRATION
}
