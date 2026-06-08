import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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
  imports: [MatButtonModule, MatIconModule, MatMenuModule],
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
            (click)="selected.emit(language.id)"
          >
            @if (language.flag) {
              <span class="lang-switcher__flag" aria-hidden="true">{{ language.flag }}</span>
            }
            <span>{{ language.label }}</span>
            @if (language.id === current()) {
              <mat-icon class="lang-switcher__check">check</mat-icon>
            }
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

    .lang-switcher__flag {
      margin-inline-end: 0.25rem;
    }

    .lang-switcher__check {
      margin-inline-start: auto;
      font-size: 1rem;
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
