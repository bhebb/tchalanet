import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

import { I18nFacade } from './i18n.facade';

const LANG_META: Record<string, { label: string; shortLabel: string; flag: string }> = {
  fr: { label: 'Français', shortLabel: 'FR', flag: '🇫🇷' },
  en: { label: 'English', shortLabel: 'EN', flag: '🇬🇧' },
  ht: { label: 'Kreyòl', shortLabel: 'HT', flag: '🇭🇹' },
};

@Component({
  selector: 'tch-language-switcher',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="language-switcher" role="group" aria-label="Language">
      @for (language of languages(); track language.id) {
        <button
          type="button"
          class="language-switcher__option"
          [class.language-switcher__option--active]="language.id === i18n.currentLanguage()"
          [attr.aria-pressed]="language.id === i18n.currentLanguage()"
          (click)="i18n.setCurrent(language.id)"
        >
          <span aria-hidden="true">{{ language.flag }}</span>
          <span>{{ language.shortLabel }}</span>
        </button>
      }
    </div>
  `,
  styles: [
    `
      .language-switcher {
        display: inline-flex;
        gap: 0.25rem;
      }

      .language-switcher__option {
        min-height: 2.25rem;
        padding: 0 0.625rem;
        border: 1px solid var(--tch-color-outline-variant, currentcolor);
        border-radius: var(--tch-radius-control, 8px);
        background: transparent;
        color: inherit;
        font: inherit;
        font-weight: 700;
        cursor: pointer;
      }

      .language-switcher__option--active {
        background: var(--tch-color-primary-container, currentcolor);
        color: var(--tch-color-on-primary-container, inherit);
      }
    `,
  ],
})
export class LanguageSwitcher {
  readonly i18n = inject(I18nFacade);

  readonly languages = () =>
    this.i18n.languages().map(id => ({
      id,
      label: LANG_META[id]?.label ?? id,
      shortLabel: LANG_META[id]?.shortLabel ?? id.toUpperCase(),
      flag: LANG_META[id]?.flag,
    }));
}
