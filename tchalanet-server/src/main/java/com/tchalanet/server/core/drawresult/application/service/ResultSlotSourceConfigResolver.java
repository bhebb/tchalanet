package com.tchalanet.server.core.drawresult.application.service;

import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@UseCase
@RequiredArgsConstructor
public class ResultSlotSourceConfigResolver {

    public ResultSlotSourceConfig resolve(JsonNode sourceCfg) {
        if (sourceCfg == null || sourceCfg.isNull() || !sourceCfg.isObject()) {
            return new ResultSlotSourceConfig(null, null);
        }

        return new ResultSlotSourceConfig(
            ResultSlotSourceConfig.SourceGame.from(sourceCfg.get("pick3")),
            ResultSlotSourceConfig.SourceGame.from(sourceCfg.get("pick4")));
    }
}
