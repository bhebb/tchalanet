import { Component, inject } from '@angular/core';

import { I18nFacade } from './i18n.facade';

@Component({
  selector: 'tch-language-switcher',
  template: `
    <div class="language-switcher" role="group" aria-label="Language">
      @for (language of i18n.languages(); track language) {
        <button
          type="button"
          [class.active]="language === i18n.currentLanguage()"
          (click)="i18n.setCurrent(language)"
        >
          {{ i18n.label(language) }}
        </button>
      }
    </div>
  `,
  styles: [
    `
      .language-switcher {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      button.active {
        font-weight: 700;
      }
    `,
  ],
})
export class LanguageSwitcherComponent {
  readonly i18n = inject(I18nFacade);
}
