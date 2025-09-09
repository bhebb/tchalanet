import { InjectionToken, Type } from '@angular/core';
import { PageElement } from '@tchl/types';

export type WidgetFactory = (el: PageElement) => Promise<Type<any>> | Type<any>;

/** Multi-provider: chaque feature peut enregistrer ses widgets ici */
export const TCH_WIDGET_REGISTRY = new InjectionToken<Record<string, WidgetFactory>>(
  'TCH_WIDGET_REGISTRY'
);
