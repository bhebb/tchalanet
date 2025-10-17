import { Provider } from '@angular/core';
import { TCH_WIDGET_REGISTRY } from './widget.token';

import {
  DrawSwitcherWidget,
  FeatureCardWidgetComponent,
  HeroWidgetComponent,
  NewsBannerWidget,
} from '@tchl/web/widgets';
import { QuickActionsWidgetComponent } from './quick-actions/quick-actions.component';

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
