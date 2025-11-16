import { Provider } from '@angular/core';

import { DrawSwitcherWidget } from './draw-switcher/draw-switcher.component';
import { FeatureCardWidgetComponent } from './feature-card/feature-card.component';
import { HeroWidgetComponent } from './hero/hero.component';
import { NewsBannerWidget } from './news-banner/news-banner.component';
import { QuickActionsWidgetComponent } from './quick-actions/quick-actions.component';
import { TCH_WIDGET_REGISTRY } from './widget.token';

export const provideBuiltinWidgets = (): Provider => [
  {
    provide: TCH_WIDGET_REGISTRY,
    multi: true,
    useValue: {
      HeroWidget: () => HeroWidgetComponent,
      NewsBannerWidget: () => NewsBannerWidget,
      DrawSwitcherWidget: () => DrawSwitcherWidget,
      FeatureCardWidget: () => FeatureCardWidgetComponent,
      KpiWidget: () => import('./kpi/kpi.component').then(m => m.KpiWidgetComponent),
      InfoWidget: () =>
        import('./info-widget/info-widget.component').then(m => m.InfoWidgetComponent),
      QuickActionsWidget: () => QuickActionsWidgetComponent,
    } as Record<string, any>,
  },
];
