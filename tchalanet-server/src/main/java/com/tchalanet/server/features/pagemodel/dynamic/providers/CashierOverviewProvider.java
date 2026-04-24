package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.pagemodel.PageModel;
import com.tchalanet.server.features.pagemodel.PageModelDynamicProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CashierOverviewProvider implements PageModelDynamicProvider {

    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
        return "private.dashboard.cashier".equals(logicalId) && "overview".equals(source);
    }

    @Override
    public Object load(PageModel pageModel, String widgetId, PageModel.WidgetConfig widgetConfig, String lang, TchRequestContext ctx) {
        // V1: payload dummy, on branchera les vrais KPIs ensuite
        return Map.of(
            "welcome", "Bienvenue",
            "ticketsToday", 0,
            "sessionOpen", true
        );
    }

    @Override
    public String providerKey() {
        return "cashier-overview";
    }
}
