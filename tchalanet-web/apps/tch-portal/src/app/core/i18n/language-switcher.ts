import { Component, inject } from '@angular/core';
import { TchLangSwitcher } from '@tch/ui/components';

import { I18nFacade } from './i18n.facade';

const LANG_META: Record<string, { label: string; shortLabel: string; flag: string }> = {
  fr: { label: 'Français', shortLabel: 'FR', flag: '🇫🇷' },
  en: { label: 'English', shortLabel: 'EN', flag: '🇬🇧' },
  ht: { label: 'Kreyòl', shortLabel: 'HT', flag: '🇭🇹' },
};

@Component({
  selector: 'tch-language-switcher',
  imports: [TchLangSwitcher],
  template: `
    <tch-lang-switcher
      [languages]="languages()"
      [current]="i18n.currentLanguage()"
      (selected)="i18n.setCurrent($event)"
    />
  `,
})
export class LanguageSwitcherComponent {
  readonly i18n = inject(I18nFacade);

  readonly languages = () =>
    this.i18n.languages().map(id => ({
      id,
      label: LANG_META[id]?.label ?? id,
      shortLabel: LANG_META[id]?.shortLabel ?? id.toUpperCase(),
      flag: LANG_META[id]?.flag,
    }));
}
