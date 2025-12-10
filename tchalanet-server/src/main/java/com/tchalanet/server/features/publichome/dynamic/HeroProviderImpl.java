package com.tchalanet.server.features.publichome.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.HeroBlock;
import org.springframework.stereotype.Component;

@Component
public class HeroProviderImpl implements HeroProvider {

    @Override
    public HeroBlock buildHeroBlock(PageModel pageModel, String currentLang) {
        String title = "Welcome to Tchalanet";
        String subtitle = "Your dynamic public home";
        String cta = "Get started";
        return new HeroBlock(title, subtitle, cta);
    }
}

