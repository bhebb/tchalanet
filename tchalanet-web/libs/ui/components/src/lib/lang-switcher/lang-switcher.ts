import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

export interface LanguageOption {
  readonly id: string;
  readonly label: string;
  readonly shortLabel?: string;
  readonly flag?: string;
}

@Component({
  selector: 'tch-lang-switcher',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatMenuModule],
  template: `
    <div class="lang-switcher">
      <button
        type="button"
        mat-icon-button
        class="lang-switcher__trigger"
        [matMenuTriggerFor]="langMenu"
        [attr.aria-label]="ariaLabel()"
      >
        <span class="lang-switcher__current" aria-hidden="true">
          @if (currentOption()?.flag) {
            {{ currentOption()!.flag }}
          }
          {{ currentOption()?.shortLabel ?? currentOption()?.id?.toUpperCase() }}
        </span>
      </button>

      <mat-menu #langMenu="matMenu" class="lang-switcher__menu">
        @for (language of languages(); track language.id) {
          <button
            mat-menu-item
            type="button"
            class="lang-switcher__item"
            [class.is-active]="language.id === current()"
            [attr.aria-current]="language.id === current() ? 'true' : null"
            (click)="selected.emit(language.id)"
          >
            <span class="lang-switcher__item-label">
              @if (language.flag) {
                <span class="lang-switcher__flag" aria-hidden="true">{{ language.flag }}</span>
              }
              {{ language.label }}
            </span>
            <span class="lang-switcher__check" aria-hidden="true">
              @if (language.id === current()) { ✓ }
            </span>
          </button>
        }
      </mat-menu>
    </div>
  `,
  styles: [`
    :host {
      --comp-lang-active: var(--tch-color-primary);
      --comp-lang-trigger-fg: var(--tch-color-on-surface-variant);
    }

    .lang-switcher {
      display: inline-flex;
      align-items: center;
    }

    .lang-switcher__trigger {
      color: var(--comp-lang-trigger-fg);
      font-size: 0.875rem;
      font-weight: 700;
    }

    .lang-switcher__current {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      font-size: 0.8125rem;
      font-weight: 700;
    }

    .lang-switcher__item.is-active {
      color: var(--comp-lang-active);
      font-weight: 700;
    }

    .lang-switcher__item-label {
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      flex: 1 1 auto;
      min-width: 0;
    }

    .lang-switcher__flag {
      flex-shrink: 0;
    }

    .lang-switcher__check {
      flex-shrink: 0;
      width: 1.25rem;
      text-align: center;
      font-size: 0.875rem;
      font-weight: 700;
      color: var(--comp-lang-active);
    }
  `],
})
export class TchLangSwitcher {
  readonly languages = input<readonly LanguageOption[]>([]);
  readonly current = input('');
  readonly ariaLabel = input('Changer la langue');
  readonly selected = output<string>();

  currentOption() {
    return this.languages().find(l => l.id === this.current());
  }
}
