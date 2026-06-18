package com.tchalanet.server.core.haiti.internal.domain.lottery.service;

import com.tchalanet.server.core.haiti.api.HaitiResult;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionToken;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public final class DefaultHaitiResultProjector implements HaitiResultProjector {

    @Override
    public HaitiResult project(HaitiProjectionConfig config, ExternalPick pick) {
        Map<HaitiLot, String> out = new LinkedHashMap<>();
        var p3 = pick.pick3();
        var p4 = pick.pick4();

        for (HaitiLot lot : HaitiLot.values()) {
            HaitiProjectionToken token = config.tokens().get(lot);
            if (token == null) {
                throw new IllegalArgumentException("Missing token for " + lot);
            }

            String value = projectToken(token, p3, p4);
            out.put(lot, value);
        }

        log.debug(
            "Projected Haiti result for pick3: {}, pick4: {}, pick: {}, result: {}",
            p3,
            p4,
            pick,
            out
        );

        return new HaitiResult(out);
    }

    private static String projectToken(HaitiProjectionToken token, String p3, String p4) {
        return switch (token) {
            case PICK3_FULL_3 -> present(p3) ? p3.trim() : "";
            case PICK3_FIRST2 -> present(p3) && p3.length() >= 2 ? p3.substring(0, 2) : "";
            case PICK3_LAST2 -> present(p3) && p3.length() >= 2 ? p3.substring(p3.length() - 2) : "";
            case PICK4_FULL_4 -> present(p4) ? p4.trim() : "";
            case PICK4_FIRST2 -> present(p4) && p4.length() >= 2 ? p4.substring(0, 2) : "";
            case PICK4_LAST2 -> present(p4) && p4.length() >= 2 ? p4.substring(p4.length() - 2) : "";
        };
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

}
