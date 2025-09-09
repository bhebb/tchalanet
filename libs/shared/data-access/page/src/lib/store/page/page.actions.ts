import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { PageModel } from '@tchl/types';

export const PageActions = createActionGroup({
  source: 'Page',
  events: {
    'Load Page': props<{ context: string; tenantId: string }>(),
    'Load Page Success': props<{ page: PageModel }>(),
    'Load Page Failure': props<{ error: unknown }>(),

    'Fallback Requested': emptyProps(),
    'Fallback Succeeded': props<{ page: PageModel }>(),

    'Navigate Not Found': emptyProps(),
  },
});
