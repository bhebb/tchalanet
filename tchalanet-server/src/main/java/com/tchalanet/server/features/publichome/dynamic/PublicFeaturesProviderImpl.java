package com.tchalanet.server.features.publichome.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.FeaturesBlock;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicFeaturesProviderImpl implements PublicFeaturesProvider {

  @Override
  public FeaturesBlock buildFeaturesBlock(PageModel pageModel, String currentLang) {
    var items =
        List.of(
            new FeaturesBlock.FeatureItem("star", "Fast", "Fast and reliable service"),
            new FeaturesBlock.FeatureItem("shield", "Secure", "Your data is secure"));
    return new FeaturesBlock(items);
  }
}
