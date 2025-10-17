import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { LangSwitcherComponent } from '@tchl/web/widgets';
import { ThemeMode } from '@tchl/types';

@Component({
  selector: 'tch-lang-theme-group',
  standalone: true,
  imports: [CommonModule, MatIconModule, LangSwitcherComponent],
  host: {
    class: 'tchl-lang-theme-group',
    role: 'group',
    'aria-label': 'Langue et thème',
    '[style.--chip-size.px]': 'size()',
    '[style.--chip-gap.rem]': 'gapRem()',
  },
  template: `
    <!-- LANG -->
    <tchl-lang-switcher
      class="chip lang"
      [currentLang]="currentLang()"
      [availableLangs]="availableLangs()"
      (change)="changeLang.emit($event)">
    </tchl-lang-switcher>

    <!-- THEME -->
    <button
      type="button"
      class="chip theme-toggle"
      [class.is-dark]="effectiveTheme() === 'dark'"
      [attr.aria-pressed]="effectiveTheme() === 'dark'"
      (click)="emitToggle()"
      (keydown.enter)="emitToggle()"
      (keydown.space)="emitToggle(); $event.preventDefault()"
      [attr.aria-label]="themeAriaLabel()"
      aria-live="polite">
      <svg class="icon sun" viewBox="0 0 24 24" aria-hidden="true">
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
      <svg class="icon moon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M21 12.6A8.5 8.5 0 1 1 11.4 3a6.7 6.7 0 1 0 9.6 9.6Z" />
      </svg>
    </button>
  `,
  styles: [`
    .tchl-lang-theme-group {
      display: inline-flex;
      align-items: center;
      gap: var(--chip-gap, 0.625rem);
      --chip-size: 40px;
      --chip-gap: 0.625rem;
      --chip-icon-scale: .86;
    }

    /* Gabarit commun : strictement rond, dimensionnée par --chip-size */
    .chip {
      inline-size: var(--chip-size);
      block-size: var(--chip-size);
      min-inline-size: var(--chip-size); /* ⬅️ plus de 40px forcé */
      min-block-size: var(--chip-size);
      border-radius: 9999px;
      display: grid;
      place-items: center;
      box-sizing: border-box;
      padding: 0;
      line-height: 1;

      /* Anneau unique, fond transparent */
      border: 1px solid color-mix(in oklab, currentColor 28%, transparent);
      background: transparent; /* ⬅️ plus de fond semi-blanc */
      color: inherit;
      cursor: pointer;
    }

    .chip:focus-visible {
      outline: 2px solid currentColor;
      outline-offset: 2px;
    }

    /* Lang switcher = même gabarit, sans padding/fond internes */
    :host ::ng-deep tchl-lang-switcher.chip {
      inline-size: var(--chip-size) !important;
      block-size: var(--chip-size) !important;
      min-inline-size: var(--chip-size) !important;
      min-block-size: var(--chip-size) !important;
      border-radius: 9999px !important;
      display: grid !important;
      place-items: center !important;
      padding: 0 !important;
      background: transparent !important;
      border: 1px solid color-mix(in oklab, currentColor 28%, transparent) !important;
      box-sizing: border-box !important;
    }

    /* Contenu interne : drapeau/icone centré, pas de fond */
    :host ::ng-deep tchl-lang-switcher.chip > :is(img, svg, .mat-icon) {
      inline-size: calc(var(--chip-size) * 0.70) !important;
      block-size: calc(var(--chip-size) * 0.70) !important;
      display: block !important;
      object-fit: contain !important;
      border-radius: 9999px !important;
      background: transparent !important;
      border: 0 !important;
    }

    /* Toggle thème (inchangé sauf fond) */
    .theme-toggle {
      position: relative;
      overflow: hidden;
    }

    .theme-toggle .icon {
      position: absolute;
      inline-size: calc(var(--chip-size) * 0.55);
      block-size: calc(var(--chip-size) * 0.55);
      transition: opacity .18s ease, transform .25s ease;
    }

    @media (prefers-reduced-motion: reduce) {
      .theme-toggle .icon {
        transition: none;
      }
    }

    .theme-toggle .sun {
      opacity: 1;
      transform: rotate(0) scale(1);
    }

    .theme-toggle .moon {
      opacity: 0;
      transform: rotate(-20deg) scale(.85);
    }

    .theme-toggle.is-dark .sun {
      opacity: 0;
      transform: rotate(20deg) scale(.85);
    }

    .theme-toggle.is-dark .moon {
      opacity: 1;
      transform: rotate(0) scale(1);
    }

    :host ::ng-deep tchl-lang-switcher.chip > :is(img, svg, .mat-icon) {
      inline-size: calc(var(--chip-size) * var(--chip-icon-scale)) !important;
      block-size: calc(var(--chip-size) * var(--chip-icon-scale)) !important;
      display: block !important;
      object-fit: contain !important;
      border-radius: 9999px !important;
      background: transparent !important;
      border: 0 !important;

      /* bonus netteté raster (si PNG/JPG) */
      image-rendering: auto;
    }

    /* Toggle thème – on ajuste aussi pour rester homogène */
    .theme-toggle .icon {
      inline-size: calc(var(--chip-size) * (var(--chip-icon-scale) - .12));
      block-size: calc(var(--chip-size) * (var(--chip-icon-scale) - .12));
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
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
    this.effectiveTheme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre'
  );
  gapRem = () => this.gap() / 16;

  emitToggle() {
    const next: ThemeMode = this.effectiveTheme() === 'dark' ? 'light' : 'dark';
    this.toggleTheme.emit(next);
  }
}
