package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.query.model.ListLatestDrawsWithResultsQuery;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.features.publicdraw.PublicDrawResultMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Provider des derniers résultats de tirages pour affichage public.
 *
 * <p>Props supportées :
 * - limit_per_slot : nombre de résultats par slot, défaut 1, max 10
 * - slot_keys : liste optionnelle, ex: ["NY_MID", "FL_EVE"] ou "NY_MID,FL_EVE"
 * - page : page à charger, défaut 0
 * - size : taille de page, défaut calculé
 * - max_items : legacy, remplacé par limit_per_slot
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawsProvider implements PageModelDynamicProvider {

    private static final int DEFAULT_LIMIT_PER_SLOT = 1;
    private static final int MAX_LIMIT_PER_SLOT = 10;

    /**
     * Quand slot_keys n'est pas fourni, on demande assez d'items pour couvrir les slots actifs
     * sans faire dépendre ce provider du catalogue result_slot.
     */
    private static final int DEFAULT_SLOT_COUNT_HINT = 20;

    private final QueryBus queryBus;
    private final PublicDrawResultMapper mapper;

    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
        return "draws".equals(source) || "results_by_game".equals(source);
    }

    @Override
    public Object load(
        PageModelDoc pageModel,
        String widgetId,
        PageModelDoc.WidgetConfig widgetConfig,
        String lang,
        TchRequestContext ctx) {

        Map<String, Object> props = widgetConfig == null ? null : widgetConfig.props();

        int limitPerSlot = normalizeLimit(readLimitPerSlot(props));
        List<String> slotKeys = readStringList(props, "slot_keys");

        int page = Math.max(0, readInt(props, "page", 0));
        int size = normalizePageSize(readInt(props, "size", defaultPageSize(limitPerSlot, slotKeys)));

        var pageable = PageRequest.of(page, size);

        try {
            TchPage<DrawSummary> drawPage =
                queryBus.send(new ListLatestDrawsWithResultsQuery(slotKeys, pageable));

            var response = mapper.toLatestPageResponse(drawPage, limitPerSlot);

            return Map.of(
                "draws", response.items(),
                "page", response.page(),
                "size", response.size(),
                "totalItems", response.totalItems(),
                "totalPages", response.totalPages(),
                "limitPerSlot", limitPerSlot);

        } catch (Exception e) {
            log.error(
                "DrawsProvider: failed to load latest draws widgetId={} page={} size={} limitPerSlot={} slotKeys={}",
                widgetId,
                page,
                size,
                limitPerSlot,
                slotKeys,
                e);

            return Map.of(
                "draws", List.of(),
                "page", page,
                "size", size,
                "totalItems", 0L,
                "totalPages", 0,
                "limitPerSlot", limitPerSlot);
        }
    }

    @Override
    public String providerKey() {
        return "draws";
    }

    private int readLimitPerSlot(Map<String, Object> props) {
        int v = readInt(props, "limit_per_slot", -1);

        if (v == -1) {
            v = readInt(props, "max_items", DEFAULT_LIMIT_PER_SLOT);

            if (props != null && props.containsKey("max_items")) {
                log.info("DrawsProvider: prop 'max_items' is deprecated, use 'limit_per_slot'");
            }
        }

        return v;
    }

    private static int normalizeLimit(int value) {
        if (value < 1) {
            return DEFAULT_LIMIT_PER_SLOT;
        }

        return Math.min(value, MAX_LIMIT_PER_SLOT);
    }

    private static int defaultPageSize(int limitPerSlot, List<String> slotKeys) {
        int slotCount = slotKeys == null || slotKeys.isEmpty()
            ? DEFAULT_SLOT_COUNT_HINT
            : slotKeys.size();

        return Math.max(1, limitPerSlot * slotCount);
    }

    private static int normalizePageSize(int value) {
        if (value < 1) {
            return DEFAULT_LIMIT_PER_SLOT * DEFAULT_SLOT_COUNT_HINT;
        }

        return Math.min(value, 200);
    }

    private static int readInt(Map<String, Object> props, String key, int def) {
        if (props == null) {
            return def;
        }

        Object v = props.get(key);

        if (v instanceof Integer i) {
            return i;
        }

        if (v instanceof Number n) {
            return n.intValue();
        }

        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignore) {
                return def;
            }
        }

        return def;
    }

    private static List<String> readStringList(Map<String, Object> props, String key) {
        if (props == null || !props.containsKey(key)) {
            return null;
        }

        Object value = props.get(key);

        if (value instanceof List<?> list) {
            var normalized =
                list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(DrawsProvider::normalizeSlotKey)
                    .filter(s -> s != null)
                    .distinct()
                    .toList();

            return normalized.isEmpty() ? null : normalized;
        }

        if (value instanceof String s) {
            var normalized =
                Arrays.stream(s.split(","))
                    .map(DrawsProvider::normalizeSlotKey)
                    .filter(v -> v != null)
                    .distinct()
                    .toList();

            return normalized.isEmpty() ? null : normalized;
        }

        return null;
    }

    private static String normalizeSlotKey(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();

        return trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
