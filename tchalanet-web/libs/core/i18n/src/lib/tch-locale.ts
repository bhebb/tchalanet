export type TchLocaleCode = 'fr' | 'ht' | 'en';

export interface TchLocaleOption {
  readonly code: TchLocaleCode;
  readonly label: string;
}

export const TCH_SUPPORTED_LOCALES: readonly TchLocaleOption[] = [
  { code: 'fr', label: 'Français' },
  { code: 'ht', label: 'Kreyòl' },
  { code: 'en', label: 'English' },
];
