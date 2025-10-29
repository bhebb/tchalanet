import { computed, effect, Injectable, signal } from '@angular/core';

import { ThemeDomApplier } from './theme.dom-applier';
import { ThemeRepository } from './theme.repository';
import { TenantThemePayload, ThemeMode } from './theme.types';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _presetId = signal<string | null>(this.restorePresetId());
  presetId = this._presetId.asReadonly();
  private readonly _mode = signal<ThemeMode>(this.restoreMode()); // 'light' | 'dark' | 'system'
  mode = this._mode.asReadonly();
  private readonly _density = signal<0 | -1 | -2>(this.restoreDensity());
  density = this._density.asReadonly();
  private readonly _overrides = signal<{ vars?: Record<string, string>; fontHref?: string } | null>(null);
  /** mode effectif (system => matchMedia) */
  private readonly effectiveMode = computed<'light' | 'dark'>(() => {
    const pref = this._mode();
    if (pref === 'light' || pref === 'dark') return pref;
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  constructor(private readonly repo: ThemeRepository, private readonly applier: ThemeDomApplier) {
    // re-réagir aux changements de prefers-color-scheme si mode='system'
    if (typeof window !== 'undefined') {
      const mq = matchMedia('(prefers-color-scheme: dark)');
      mq.addEventListener?.('change', () => {
        // retrigger computed effectiveMode
        this._mode.set(this._mode());
      });
    }

    // effet principal : applique le thème dès qu'un bout change
    effect(() => {
      if (!this.repo.ready()) return;

      const id = this._presetId() ?? 'tchalanet';
      const preset = this.repo.get(id) ?? this.repo.get('tchalanet');
      if (!preset) return; // worst case

      const mode = this.effectiveMode();
      const density = this._density();
      const overrides = this._overrides();

      // 1. injecter le CSS du preset
      this.applier.applyPresetCss(id, preset!.css);

      // 2. appliquer l'état runtime (mode, density, overrides)
      this.applier.applyRuntimeState({
        mode,
        density,
        overrides: overrides ?? undefined,
      });

      // 3. persister
      this.persist();
    });
  }

  // ------ API public ------

  setPreset(id: string) {
    if (!this.repo.has(id)) {
      console.warn('[ThemeService] preset inconnu:', id);
      return;
    }
    this._presetId.set(id);
  }

  setMode(mode: ThemeMode) {
    this._mode.set(mode);
  }

  toggleDark() {
    const eff = this.effectiveMode();
    this.setMode(eff === 'dark' ? 'light' : 'dark');
  }

  setDensity(d: 0 | -1 | -2) {
    this._density.set(d);
  }


  applyTheme(payload:TenantThemePayload) {
    if (payload.presetId) this._presetId.set(payload.presetId);
    if (payload.mode) this._mode.set(payload.mode);
    if (payload.density != null) this._density.set(payload.density);
    if (payload.overrides) this._overrides.set(payload.overrides);
  }

  // ------ Persistence helpers ------

  private persist() {
    try {
      localStorage.setItem('tch.theme.mode', this._mode());
      localStorage.setItem('tch.theme.density', String(this._density()));
      const id = this._presetId();
      if (id) localStorage.setItem('tch.theme.preset', id);
    } catch {}
  }

  private restoreMode(): ThemeMode {
    try {
      const v = localStorage.getItem('tch.theme.mode') as ThemeMode | null;
      return v === 'light' || v === 'dark' || v === 'system' ? v : 'system';
    } catch {
      return 'system';
    }
  }

  private restorePresetId(): string | null {
    try {
      const v = localStorage.getItem('tch.theme.preset');
      return v && v.trim().length ? v : 'tchalanet';
    } catch {
      return 'tchalanet';
    }
  }

  private restoreDensity(): 0 | -1 | -2 {
    try {
      const n = Number(localStorage.getItem('tch.theme.density'));
      return n === -1 || n === -2 ? (n as -1 | -2) : 0;
    } catch {
      return 0;
    }
  }
}
