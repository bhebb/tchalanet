import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface LanguageOption {
  readonly id: string;
  readonly label: string;
}

@Component({
  selector: 'tch-lang-switcher',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="lang-switcher" role="group" [attr.aria-label]="ariaLabel()">
      @for (language of languages(); track language.id) {
        <button type="button" [class.is-active]="language.id === current()" (click)="selected.emit(language.id)">
          {{ language.label }}
        </button>
      }
    </div>
  `,
  styles: [`
    :host { --comp-lang-active: var(--tch-color-primary); }
    .lang-switcher { display: flex; gap: .375rem; }
    button { border: 0; border-radius: var(--tch-radius-pill); background: transparent; color: inherit; cursor: pointer; padding: .5rem; }
    button.is-active { color: var(--comp-lang-active); font-weight: 800; }
  `],
})
export class TchLangSwitcher {
  readonly languages = input<readonly LanguageOption[]>([]);
  readonly current = input('');
  readonly ariaLabel = input('Language');
  readonly selected = output<string>();
}
