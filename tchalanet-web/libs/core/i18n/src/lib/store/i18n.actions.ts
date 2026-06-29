import { createActionGroup, props } from '@ngrx/store';

export const I18nActions = createActionGroup({
  source: 'I18n',
  events: {
    Init: props<{ languages: readonly string[]; defaultLanguage: string }>(),
    'Set Current': props<{ language: string }>(),
    'Set Error': props<{ error: unknown }>(),
    'Set Languages': props<{ languages: readonly string[]; currentLanguage?: string }>(),
  },
});
