export type TranslationValue = string | TranslationTree;

export interface TranslationTree {
  readonly [key: string]: TranslationValue;
}

export interface I18nBundle {
  readonly language: string;
  readonly translations: TranslationTree;
}

export interface I18nOverrides {
  readonly language: string;
  readonly scope: string;
  readonly translations: TranslationTree;
  readonly version?: string;
}
