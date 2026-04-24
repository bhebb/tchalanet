package com.tchalanet.server.features.pagemodel_backup.shared.block;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicPageDynamicPayload(
    HeroBlock hero,
    FeaturesBlock features,
    PlansBlock plans,
    NewsBlock news,
    ResultsByGameBlock resultsByGame)
    implements TchalaBlock {}
