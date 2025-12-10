package com.tchalanet.server.features.pagemodel.shared.block;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HeroBlock(
    String title,
    String subtitle,
    String cta
) {
}
