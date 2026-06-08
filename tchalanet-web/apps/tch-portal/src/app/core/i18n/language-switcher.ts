import { Component, inject } from '@angular/core';
import { TchLangSwitcher } from '@tch/ui/components';

import { I18nFacade } from './i18n.facade';

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
    this.i18n.languages().map((id) => ({ id, label: this.i18n.label(id) }));
}
