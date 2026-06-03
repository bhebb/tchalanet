import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { ThemeMode } from '../../shared/types';
import { ThemeRuntimeStore } from './theme-runtime.store';

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
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      select {
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-control);
        background: var(--tch-color-surface);
        color: var(--tch-color-foreground);
        padding: 0.45rem 0.6rem;
      }
    `,
  ],
})
export class ThemeSwitcherComponent {
  readonly theme = inject(ThemeRuntimeStore);
  readonly modes: readonly ThemeMode[] = ['light', 'dark', 'system'];
}
