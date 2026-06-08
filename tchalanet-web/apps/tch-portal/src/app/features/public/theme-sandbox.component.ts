// Theme dev sandbox — toggleable panel to exercise the theme (presets, light/dark, density, M3 type
// scale, fonts) and inspect colour roles against real Material components. Dev-only: the launcher is
// hidden unless isDevMode(). Kept on purpose; activate with the floating 🧪 button.
import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, isDevMode, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ThemeDensity, ThemeMode, ThemeStore } from '@tch/ui/theme';

interface Swatch {
  readonly label: string;
  readonly token: string;
  readonly value: string;
}

const COLOR_TOKENS: readonly string[] = [
  '--tch-color-primary',
  '--tch-color-on-primary',
  '--tch-color-primary-container',
  '--tch-color-secondary',
  '--tch-color-secondary-container',
  '--tch-color-accent',
  '--tch-color-surface',
  '--tch-color-surface-container',
  '--tch-color-on-surface',
  '--tch-color-outline',
  '--tch-color-error',
  '--tch-color-background',
  '--tch-header-bg',
  '--tch-footer-bg',
  '--mat-sys-primary',
  '--mat-sys-secondary',
  '--mat-sys-tertiary',
];

const STORAGE_KEY = 'tch.theme-sandbox.open';

@Component({
  selector: 'tch-theme-sandbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
  ],
  template: `
    @if (devMode) {
      <button type="button" class="launcher" (click)="toggle()" [attr.aria-expanded]="open()">
        🧪 {{ open() ? 'Fermer' : 'Theme' }}
      </button>

      @if (open()) {
        <aside class="panel" role="dialog" aria-label="Theme sandbox">
          <header class="panel__bar">
            <strong>🧪 Theme sandbox</strong>
            <label>
              Preset
              <select [ngModel]="theme.activeTheme().activePresetKey" (ngModelChange)="setPreset($event)">
                @for (p of theme.presets(); track p.id) {
                  <option [value]="p.id">{{ p.id }}</option>
                }
              </select>
            </label>
            <label>
              Mode
              <select [ngModel]="theme.activeTheme().mode" (ngModelChange)="setMode($event)">
                @for (m of modes; track m) {
                  <option [value]="m">{{ m }}</option>
                }
              </select>
            </label>
            <label>
              Densité
              <select [ngModel]="theme.activeTheme().density" (ngModelChange)="setDensity($event)">
                @for (d of densities; track d) {
                  <option [value]="d">{{ d }}</option>
                }
              </select>
            </label>
            <button type="button" (click)="refresh()">↻</button>
          </header>

          <section class="block">
            <h4>Couleurs (rôles)</h4>
            <div class="swatches">
              @for (s of swatches(); track s.token) {
                <div class="swatch">
                  <span class="swatch__chip" [style.background]="'var(' + s.token + ')'"></span>
                  <span class="swatch__meta">
                    <code>{{ s.label }}</code>
                    <code class="swatch__val">{{ s.value }}</code>
                  </span>
                </div>
              }
            </div>
          </section>

          <section class="block">
            <h4>Typo (échelle M3 bridgée)</h4>
            <p style="font-size: var(--tch-font-size-display-lg); line-height: var(--tch-line-height-display-lg)">Display</p>
            <p style="font-size: var(--tch-font-size-headline-lg)">Headline</p>
            <p style="font-size: var(--tch-font-size-title-md)">Title</p>
            <p style="font-size: var(--tch-font-size-body-md)">Body — <span style="font-family: var(--tch-font-family)">police active</span></p>
          </section>

          <section class="block">
            <h4>Composants Material (densité)</h4>
            <div class="row">
              <button mat-flat-button color="primary">Filled</button>
              <button mat-stroked-button>Outlined</button>
              <button mat-button>Text</button>
            </div>
            <mat-form-field appearance="outline">
              <mat-label>Champ</mat-label>
              <input matInput placeholder="tape ici" />
            </mat-form-field>
            <div class="row">
              <mat-checkbox checked>Case</mat-checkbox>
              <mat-slide-toggle checked>Toggle</mat-slide-toggle>
            </div>
            <mat-chip-set>
              <mat-chip>Chip A</mat-chip>
              <mat-chip>Chip B</mat-chip>
            </mat-chip-set>
          </section>
        </aside>
      }
    }
  `,
  styles: [
    `
      .launcher {
        position: fixed;
        right: 1rem;
        bottom: 1rem;
        z-index: var(--tch-z-toast, 60);
        padding: 0.5rem 0.8rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-pill);
        background: var(--tch-color-surface-container-high);
        color: var(--tch-color-on-surface);
        cursor: pointer;
        box-shadow: var(--tch-elevation-2);
      }
      .panel {
        position: fixed;
        right: 1rem;
        bottom: 4rem;
        z-index: var(--tch-z-toast, 60);
        width: min(92vw, 26rem);
        max-height: 80dvh;
        overflow: auto;
        padding: 1rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-lg);
        background: var(--tch-color-surface);
        color: var(--tch-color-on-surface);
        box-shadow: var(--tch-elevation-3);
      }
      .panel__bar {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
      }
      .panel__bar label {
        display: inline-flex;
        gap: 0.3rem;
        align-items: center;
        font-size: 0.8rem;
      }
      select,
      .panel__bar button {
        padding: 0.3rem 0.45rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-md);
        background: var(--tch-color-surface);
        color: var(--tch-color-on-surface);
      }
      .block {
        padding-top: 0.5rem;
        border-top: 1px solid var(--tch-color-outline-variant);
      }
      .block h4 {
        margin: 0.5rem 0;
        font-size: 0.85rem;
        color: var(--tch-color-on-surface-variant);
      }
      .swatches {
        display: grid;
        gap: 0.4rem;
        grid-template-columns: 1fr 1fr;
      }
      .swatch {
        display: flex;
        align-items: center;
        gap: 0.4rem;
      }
      .swatch__chip {
        flex: 0 0 auto;
        width: 1.6rem;
        height: 1.6rem;
        border-radius: var(--tch-radius-sm);
        border: 1px solid var(--tch-color-outline-variant);
      }
      .swatch__meta {
        display: flex;
        flex-direction: column;
        min-width: 0;
      }
      .swatch__meta code {
        font-size: 0.62rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .swatch__val {
        color: var(--tch-color-on-surface-variant);
      }
      .row {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: center;
        margin: 0.4rem 0;
      }
      p {
        margin: 0.25rem 0;
      }
    `,
  ],
})
export class ThemeSandboxComponent {
  private readonly document = inject(DOCUMENT);
  readonly theme = inject(ThemeStore);
  readonly devMode = isDevMode();
  readonly modes: readonly ThemeMode[] = ['light', 'dark', 'system'];
  readonly densities: readonly ThemeDensity[] = ['comfortable', 'compact', 'dense'];
  readonly open = signal(this.restoreOpen());
  private readonly tick = signal(0);

  readonly swatches = computed<readonly Swatch[]>(() => {
    this.tick(); // recompute when controls change / refresh
    if (!this.open()) {
      return [];
    }
    const cs = getComputedStyle(this.document.body);
    return COLOR_TOKENS.map((token) => ({
      label: token.replace('--tch-', '').replace('--mat-sys-', 'mat:'),
      token,
      value: cs.getPropertyValue(token).trim() || '—',
    }));
  });

  toggle(): void {
    const next = !this.open();
    this.open.set(next);
    localStorage.setItem(STORAGE_KEY, next ? '1' : '0');
    this.refresh();
  }

  setPreset(value: string): void {
    this.theme.setPreset(value);
    this.refresh();
  }

  setMode(value: ThemeMode): void {
    this.theme.setMode(value);
    this.refresh();
  }

  setDensity(value: ThemeDensity): void {
    this.theme.setDensity(value);
    this.refresh();
  }

  refresh(): void {
    // Defer so the DOM/theme classes are applied before we read computed values.
    setTimeout(() => this.tick.update((n) => n + 1));
  }

  private restoreOpen(): boolean {
    return localStorage.getItem(STORAGE_KEY) === '1';
  }
}
