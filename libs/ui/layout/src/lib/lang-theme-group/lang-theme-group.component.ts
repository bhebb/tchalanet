import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ThemeMode } from '@tchl/ui/theme';

import { LangSwitcherComponent } from '../lang-switcher/lang-switcher.component';

@Component({
  selector: 'tchl-lang-theme-group',
  standalone: true,
  imports: [CommonModule, MatIconModule, LangSwitcherComponent],
  host: {
    class: 'tch-ltg', // bloc BEM du group
    role: 'group',
    'aria-label': 'Langue et thème',
    '[style.--comp-ltg-size.px]': 'size()',
    '[style.--comp-ltg-gap.rem]': 'gapRem()',
  },
  template: `
    <!-- Lang chip -->
    <tchl-lang-switcher
      class="tch-ltg__chip tch-ltg__chip--lang"
      [currentLang]="currentLang()"
      [availableLangs]="availableLangs()"
      (change)="changeLang.emit($event)"
    >
    </tchl-lang-switcher>

    <!-- Theme chip -->
    <button
      type="button"
      class="tch-ltg__chip tch-ltg__chip--theme"
      [class.tch-ltg__chip--theme-dark]="effectiveTheme() === 'dark'"
      [attr.aria-pressed]="effectiveTheme() === 'dark'"
      (click)="emitToggle()"
      (keydown.enter)="emitToggle()"
      (keydown.space)="emitToggle(); $event.preventDefault()"
      [attr.aria-label]="themeAriaLabel()"
      aria-live="polite"
    >
      <svg class="tch-ltg__icon tch-ltg__icon--sun" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <g stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
          <line x1="12" y1="2.5" x2="12" y2="5.2" />
          <line x1="12" y1="18.8" x2="12" y2="21.5" />
          <line x1="2.5" y1="12" x2="5.2" y2="12" />
          <line x1="18.8" y1="12" x2="21.5" y2="12" />
          <line x1="4.6" y1="4.6" x2="6.6" y2="6.6" />
          <line x1="17.4" y1="17.4" x2="19.4" y2="19.4" />
          <line x1="17.4" y1="6.6" x2="19.4" y2="4.6" />
          <line x1="4.6" y1="19.4" x2="6.6" y2="17.4" />
        </g>
      </svg>

      <svg class="tch-ltg__icon tch-ltg__icon--moon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M21 12.6A8.5 8.5 0 1 1 11.4 3a6.7 6.7 0 1 0 9.6 9.6Z" />
      </svg>
    </button>
  `,
  styles: [
    `
      :host {
        /* sizing / layout interne */
        --comp-ltg-size: 40px;
        --comp-ltg-gap: 0.625rem; /* 10px */
        --comp-ltg-icon-scale: 0.86;

        /* visuel des chips (hérité / overridable par le parent, ex header) */
        --comp-ltg-chip-bg: transparent;
        --comp-ltg-chip-fg: currentColor;
        --comp-ltg-chip-border-color: color-mix(in oklab, currentColor 28%, transparent);
        --comp-ltg-chip-focus-ring: currentColor;

        display: inline-flex;
        align-items: center;
        gap: var(--comp-ltg-gap);
      }

      /* ============================================
       Élément chip (lang ou theme)
       ============================================ */
      .tch-ltg__chip {
        inline-size: var(--comp-ltg-size);
        block-size: var(--comp-ltg-size);
        min-inline-size: var(--comp-ltg-size);
        min-block-size: var(--comp-ltg-size);

        border-radius: 9999px;
        display: grid;
        place-items: center;
        box-sizing: border-box;
        padding: 0;
        line-height: 1;

        background: var(--comp-ltg-chip-bg);
        color: var(--comp-ltg-chip-fg);
        border: 1px solid var(--comp-ltg-chip-border-color);

        cursor: pointer;

        &:focus-visible {
          outline: var(--tch-focus-ring-width, 2px) solid var(--comp-ltg-chip-focus-ring);
          outline-offset: var(--tch-focus-ring-offset, 2px);
        }
      }

      /* --------------------------------------------
       Lang chip (c'est le tchl-lang-switcher host)
       :host ::ng-deep -> on force ses dimensions pour matcher parfaitement
       -------------------------------------------- */
      :host ::ng-deep tchl-lang-switcher.tch-ltg__chip--lang {
        inline-size: var(--comp-ltg-size);
        block-size: var(--comp-ltg-size);
        min-inline-size: var(--comp-ltg-size);
        min-block-size: var(--comp-ltg-size);
        border-radius: 9999px;
        display: grid;
        place-items: center;
      }

      /* --------------------------------------------
       Theme chip
       -------------------------------------------- */
      .tch-ltg__chip--theme {
        position: relative;
        overflow: hidden;
      }

      /* Icônes soleil / lune */
      .tch-ltg__icon {
        position: absolute;
        inline-size: calc(var(--comp-ltg-size) * (var(--comp-ltg-icon-scale) - 0.12));
        block-size: calc(var(--comp-ltg-size) * (var(--comp-ltg-icon-scale) - 0.12));
        transition: opacity 0.18s ease, transform 0.25s ease;
      }

      @media (prefers-reduced-motion: reduce) {
        .tch-ltg__icon {
          transition: none;
        }
      }

      /* Par défaut: afficher lune, cacher soleil */
      .tch-ltg__icon--sun {
        opacity: 0;
        transform: rotate(15deg) scale(0.9);
      }

      .tch-ltg__icon--moon {
        opacity: 1;
        transform: rotate(0) scale(1);
      }

      /* Quand on est en mode dark, on inverse */
      .tch-ltg__chip--theme-dark .tch-ltg__icon--sun {
        opacity: 1;
        transform: rotate(0) scale(1);
      }

      .tch-ltg__chip--theme-dark .tch-ltg__icon--moon {
        opacity: 0;
        transform: rotate(-15deg) scale(0.9);
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LangThemeGroupComponent {
  // Inputs contrôlés
  currentLang = input.required<string>();
  availableLangs = input.required<string[]>();
  themeMode = input<ThemeMode>('light');
  size = input<number>(40);
  gap = input<number>(10);

  // Outputs
  changeLang = output<string>();
  toggleTheme = output<ThemeMode>();

  // Dérivées d’affichage
  effectiveTheme = computed<ThemeMode>(() => this.themeMode() ?? 'light');
  themeAriaLabel = computed(() =>
    this.effectiveTheme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre',
  );
  gapRem = () => this.gap() / 16;

  emitToggle() {
    const next: ThemeMode = this.effectiveTheme() === 'dark' ? 'light' : 'dark';
    this.toggleTheme.emit(next);
  }
}
