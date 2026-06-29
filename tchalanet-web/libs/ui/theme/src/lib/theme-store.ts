import { Injectable, computed, inject, signal } from '@angular/core';

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

  /**
   * Apply a theme delivered inside the runtime bootstrap response — no extra theme HTTP call.
   * `presetCode`/`tokens` come from the private bootstrap; public bootstrap may pass mode only.
   */
  applyBootstrapTheme(input: {
    presetCode?: string | null;
    mode?: string | null;
    tokens?: Readonly<Record<string, string>> | null;
  }): void {
    const mode = normalizeMode(input.mode);
    const theme: RuntimeTheme = {
      activePresetKey: input.presetCode || this.repository.defaultPreset().id,
      mode,
      effectiveMode: this.effectiveMode(mode),
      density: this.activeTheme().density,
      tokens: { ...(input.tokens ?? {}) },
    };
    this.apply(this.resolveBackendTheme(theme));
    this.loadStateSignal.set('ready');
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
      // Preserve backend tenant token overrides across a preset switch — only the backend/runtime
      // load replaces them. Wiping here would silently drop a tenant's customised tokens.
      tokens: this.activeTheme().tokens,
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
    let resolved = theme;
    let preset = this.repository.get(theme.activePresetKey);
    if (!preset) {
      // Unknown preset code (e.g. the seeded `tchalanet_default`, which has no registry entry):
      // adopt the default preset's identity so brand tokens (--tch-*) AND Material system tokens
      // (--mat-sys-*) both resolve. The previous aliasing path rewrote the CSS selector but never
      // matched the injected `data-preset`, leaving the private shell completely unthemed
      // (white identity card, native form fields, no gold nav). Backend token overrides are
      // preserved — they re-apply under the resolved preset id.
      preset = this.repository.defaultPreset();
      resolved = { ...theme, activePresetKey: preset.id };
    }

    this.activeThemeSignal.set(resolved);
    this.dom.apply(resolved, preset.css);
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
  if (typeof localStorage === 'undefined') {
    return;
  }

  localStorage.setItem(themeModeStorageKey, theme.mode);
  localStorage.setItem(themePresetStorageKey, theme.activePresetKey);
  localStorage.setItem(themeDensityStorageKey, theme.density);
}

function restoreMode(): ThemeMode {
  const value = typeof localStorage === 'undefined' ? null : localStorage.getItem(themeModeStorageKey);
  return value === 'dark' || value === 'system' ? value : 'light';
}

function restorePresetKey(): string {
  return typeof localStorage === 'undefined'
    ? defaultThemePresetId
    : localStorage.getItem(themePresetStorageKey) || defaultThemePresetId;
}

function restoreDensity(): ThemeDensity {
  const value = typeof localStorage === 'undefined'
    ? null
    : localStorage.getItem(themeDensityStorageKey);
  return value === 'compact' || value === 'dense' ? value : 'comfortable';
}

function normalizeMode(value: string | null | undefined): ThemeMode {
  return value === 'dark' || value === 'system' ? value : 'light';
}

function systemPrefersDark(): boolean {
  return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

function normalizePresetKey(value: string): string {
  return value === 'tchalanet_default' ? defaultThemePresetId : value;
}
