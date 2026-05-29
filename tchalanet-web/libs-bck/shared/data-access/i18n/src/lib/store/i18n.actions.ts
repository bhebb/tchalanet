import { createActionGroup, props } from '@ngrx/store';

export const I18nActions = createActionGroup({
  source: 'I18n',
  events: {
    'Init From Page': props<{ langs: string[]; current: string }>(),
    'Set Current': props<{ lang: string }>(),
  },
});
