// i18n.reducer.ts
export interface I18nState {
  langs: string[]; // ex: ['fr','en','es']
  current: string; // ex: 'fr'
  error: unknown | null;
}
export const i18nInitial: I18nState = { langs: ['fr', 'en', 'ht'], current: 'fr', error: null };
