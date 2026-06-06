import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { ThemeStore } from './theme-store';
import { ThemeMode } from './theme-types';

@Component({
  imports: [FormsModule, TranslatePipe],
  selector: 'tch-theme-switcher',
  template: `
    <div class="theme-switcher" role="group" aria-label="Theme">
      <select
        [ngModel]="theme.activeTheme().activePresetKey"
        (ngModelChange)="theme.setPreset($event)"
        aria-label="Theme preset"
      >
        @for (preset of theme.presets(); track preset.id) {
          <option [value]="preset.id">{{ preset.labelKey | translate }}</option>
        }
      </select>

      <select
        [ngModel]="theme.activeTheme().mode"
        (ngModelChange)="theme.setMode($event)"
        aria-label="Theme mode"
      >
        @for (mode of modes; track mode) {
          <option [value]="mode">{{ 'theme.modes.' + mode | translate }}</option>
        }
      </select>
    </div>
  `,
  styles: [
    `
      .theme-switcher {
        --comp-theme-control-bg: var(--tch-color-surface);
        --comp-theme-control-fg: var(--tch-color-on-surface);
        --comp-theme-control-border: var(--tch-color-outline);
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      select {
        border: 1px solid var(--comp-theme-control-border);
        border-radius: var(--tch-radius-md);
        background: var(--comp-theme-control-bg);
        color: var(--comp-theme-control-fg);
        padding: 0.45rem 0.6rem;
      }
    `,
  ],
})
export class ThemeSwitcherComponent {
  readonly theme = inject(ThemeStore);
  readonly modes: readonly ThemeMode[] = ['light', 'dark', 'system'];
}
