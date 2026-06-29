export interface I18nState {
  readonly languages: readonly string[];
  readonly currentLanguage: string;
  readonly initialized: boolean;
  readonly error: unknown | null;
}

export const i18nInitialState: I18nState = {
  currentLanguage: 'fr',
  error: null,
  initialized: false,
  languages: ['fr', 'en', 'ht'],
};
