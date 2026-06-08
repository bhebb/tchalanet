import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';

import { ThemeApi } from './theme-api';
import { ThemeDomApplier } from './theme-dom-applier';
import { defaultThemePresetId } from './theme-presets';
import { ThemeRepository } from './theme.repository';
import { RuntimeTheme, ThemeDensity, ThemeMode, ThemePreset } from './theme-types';

type ThemeLoadState = 'idle' | 'loading' | 'ready' | 'fallback';

const themeModeStorageKey = 'tchalanet.web.theme.mode';
const themePresetStorageKey = 'tchalanet.web.theme.preset';
const themeDensityStorageKey = 'tchalanet.web.theme.density';

@Injectable()
export class ThemeStore {
  private readonly api = inject(ThemeApi);
  private readonly dom = inject(ThemeDomApplier);
  private readonly repository = inject(ThemeRepository);
  private readonly activeThemeSignal = signal<RuntimeTheme>(this.defaultRuntimeTheme());
  private readonly loadStateSignal = signal<ThemeLoadState>('idle');
  private readonly presetsSignal = signal<readonly ThemePreset[]>(this.repository.list());
  private readonly systemDarkSignal = signal(systemPrefersDark());

  readonly activeTheme = this.activeThemeSignal.asReadonly();
  readonly loadState = this.loadStateSignal.asReadonly();
  readonly presets = this.presetsSignal.asReadonly();
  readonly activePreset = computed(() =>
    this.presets().find((preset) => preset.id === this.activeTheme().activePresetKey),
  );

  constructor() {
    const query = globalThis.matchMedia?.('(prefers-color-scheme: dark)');
    query?.addEventListener('change', (event) => {
      this.systemDarkSignal.set(event.matches);
      if (this.activeTheme().mode === 'system') {
        this.setMode('system');
      }
    });
  }

  init(): void {
    this.apply(this.resolveStoredTheme());
  }

  loadPublicTheme(): void {
    this.loadStateSignal.set('loading');
    this.api
      .getPublicTheme(this.activeTheme().mode)
      .pipe(
        tap((theme) => {
          this.apply(this.resolveBackendTheme(theme));
          this.loadStateSignal.set('ready');
        }),
        catchError(() => {
          this.apply(this.resolveStoredTheme());
          this.loadStateSignal.set('fallback');
          return of(null);
        }),
      )
      .subscribe();
  }

  loadPrivateTheme(): void {
    this.loadStateSignal.set('loading');
    this.api
      .getPrivateTheme(this.activeTheme().mode)
      .pipe(
        tap((theme) => {
          this.apply(this.resolveBackendTheme(theme));
          this.loadStateSignal.set('ready');
        }),
        catchError(() => {
          this.apply(this.resolveStoredTheme());
          this.loadStateSignal.set('fallback');
          return of(null);
        }),
      )
      .subscribe();
  }

  setPreset(presetKey: string): void {
    const preset = this.repository.get(presetKey);
    if (!preset) {
      return;
    }

    this.apply({
      activePresetKey: preset.id,
      mode: this.activeTheme().mode,
      effectiveMode: this.effectiveMode(this.activeTheme().mode),
      density: this.activeTheme().density,
      tokens: {},
    });
    persistTheme(this.activeTheme());
  }

  setMode(mode: ThemeMode): void {
    const current = this.activeTheme();

    this.apply({
      ...current,
      effectiveMode: this.effectiveMode(mode),
      mode,
    });
    persistTheme(this.activeTheme());
  }

  setDensity(density: ThemeDensity): void {
    this.apply({ ...this.activeTheme(), density });
    persistTheme(this.activeTheme());
  }

  private effectiveMode(mode: ThemeMode): Exclude<ThemeMode, 'system'> {
    return mode === 'system' ? (this.systemDarkSignal() ? 'dark' : 'light') : mode;
  }

  private apply(theme: RuntimeTheme): void {
    const preset = this.repository.get(theme.activePresetKey);
    const defaultPreset = this.repository.defaultPreset();
    const presetCss = preset?.css ?? aliasPresetCss(defaultPreset.css, defaultPreset.id, theme.activePresetKey);

    this.activeThemeSignal.set(theme);
    this.dom.apply(theme, presetCss);
  }

  private defaultRuntimeTheme(): RuntimeTheme {
    const preset = this.repository.defaultPreset();
    return {
      activePresetKey: preset.id,
      mode: 'light',
      effectiveMode: 'light',
      density: 'comfortable',
      tokens: {},
    };
  }

  private resolveStoredTheme(): RuntimeTheme {
    const mode = restoreMode();
    const presetKey = restorePresetKey();
    const preset = this.repository.get(presetKey) ?? this.repository.defaultPreset();

    return {
      activePresetKey: preset.id,
      mode,
      effectiveMode: this.effectiveMode(mode),
      density: restoreDensity(),
      tokens: {},
    };
  }

  private resolveBackendTheme(theme: RuntimeTheme): RuntimeTheme {
    const activePresetKey = normalizePresetKey(
      theme.activePresetKey || this.repository.defaultPreset().id,
    );

    return {
      ...theme,
      effectiveMode: this.effectiveMode(theme.mode),
      activePresetKey,
      tokens: theme.tokens,
    };
  }
}

export const themeStoreProvider = {
  provide: ThemeStore,
  useClass: ThemeStore,
};

function persistTheme(theme: RuntimeTheme): void {
  localStorage.setItem(themeModeStorageKey, theme.mode);
  localStorage.setItem(themePresetStorageKey, theme.activePresetKey);
  localStorage.setItem(themeDensityStorageKey, theme.density);
}

function restoreMode(): ThemeMode {
  const value = localStorage.getItem(themeModeStorageKey);
  return value === 'dark' || value === 'system' ? value : 'light';
}

function restorePresetKey(): string {
  return localStorage.getItem(themePresetStorageKey) || defaultThemePresetId;
}

function restoreDensity(): ThemeDensity {
  const value = localStorage.getItem(themeDensityStorageKey);
  return value === 'compact' || value === 'dense' ? value : 'comfortable';
}

function systemPrefersDark(): boolean {
  return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

function aliasPresetCss(css: string, sourceId: string, targetId: string): string {
  return css.split(`[data-preset='${sourceId}']`).join(`[data-preset='${targetId}']`);
}

function normalizePresetKey(value: string): string {
  return value === 'tchalanet_default' ? defaultThemePresetId : value;
}
