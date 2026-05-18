package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@UseCase
@RequiredArgsConstructor
public class ResultSlotSourceConfigResolver {

    public ResultSlotSourceConfig resolve(JsonNode sourceCfg) {
        if (sourceCfg == null || sourceCfg.isNull() || !sourceCfg.isObject()) {
            return ResultSlotSourceConfig.empty();
        }

        return new ResultSlotSourceConfig(
            ResultSlotSourceConfig.providerSlotCodeFrom(sourceCfg),
            ResultSlotSourceConfig.SourceGame.from(sourceCfg.get("pick3")),
            ResultSlotSourceConfig.SourceGame.from(sourceCfg.get("pick4"))
        );
    }
}
