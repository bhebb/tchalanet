import { computed, effect, Inject, Injectable, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ThemeRepository } from './theme.repository';
import { ThemeDomApplier } from './theme.dom-applier';
import { ThemeAdapter } from './theme.adapter';
import { TchTheme, ThemeMode } from '@tchl/types';

const LS_MODE = 'tch.theme.mode'; // 'light'|'dark'|'system'
const LS_PRESET = 'tch.theme.preset'; // id du thème actif
const LS_DENSE = 'tch.theme.density'; // 0|-1|-2

@Injectable({ providedIn: 'root' })
export class ThemeService {
  // ======== State (signals) ========
  private _presetId = signal<string | null>(this.restorePresetId());
  private _mode = signal<ThemeMode>(this.restoreMode());
  private _density = signal<0 | -1 | -2>(this.restoreDensity());

  /** lecture seule */
  presetId = this._presetId.asReadonly();
  mode = this._mode.asReadonly();
  density = this._density.asReadonly();

  /** Thème actif résolu depuis le repo (fallback tchalanet si absent) */
  activeTheme = computed<TchTheme>(() => {
    // ⚠️ on lit ready/version pour que le computed se réévalue quand le repo change
    const _ready = this.repo.ready();
    const _ver = this.repo.version();

    const raw = this._presetId();
    const id = raw && raw.trim().length ? raw.trim().toLowerCase() : null;
    const found = id ? this.repo.get(id) : null;

    return found ?? this.ensureDefault();
  });

  /** Mode effectif en tenant compte de 'system' */
  private effectiveMode = computed<'light' | 'dark'>(() => this.resolveEffectiveMode(this._mode()));

    constructor(
    private repo: ThemeRepository,
    private applier: ThemeDomApplier,
    private adapter: ThemeAdapter,
    @Inject(DOCUMENT) private doc: Document,
  ) {
    // re-réagir si 'system' change
    if (typeof window !== 'undefined') {
      const mq = matchMedia('(prefers-color-scheme: dark)');
      mq.addEventListener?.('change', () => this._presetId.set(this._presetId()));
    }

    effect(() => {
      // 🛑 Pas d’application tant que le repo n’est pas prêt
      if (!this.repo.ready()) return;

      const theme = this.activeTheme();     // now tracks repo.ready/version + presetId
      const mode = this.effectiveMode();    // tracks user/system
      const density = this._density();

      this.applier.apply(theme, mode, density);
      // Persistance ne doit PAS toucher de signaux -> no loop
      this.persist();
    });
  }

  // ========= API contrôle =========

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

  // ========= API métier : application depuis backend =========

  /**
   * Page publique : payload du BFF { presetId, mode, primaryColor, accentColor, ... }.
   * - si presetId connu => setPreset + merge overrides
   * - sinon, on essaie de charger du repo (déjà préchargé par APP_INITIALIZER)
   */
  applyPublicTheme(payload?: TchTheme) {
    this.applyPayload(payload);
  }

  /** Après login/tenant : même logique, tu peux ajouter de la persistance si besoin */
  applyTenantTheme(payload?: TchTheme) {
    this.applyPayload(payload);
  }

  private applyPayload(payload?: TchTheme) {
    // 1) choisir le preset de référence
    const presetId = payload?.id ?? this._presetId() ?? 'tchalanet';
    if (!this.repo.has(presetId)) {
      console.warn('[ThemeService] preset absent du repo:', presetId, '→ fallback tchalanet');
    }
    const base = this.repo.get(presetId) ?? this.ensureDefault();

    // 2) fusionner les overrides backend
    const merged = this.adapter.merge(base, payload);

    // 3) on upsert sous le même id (ou un id dérivé si tu veux différencier)
    this.repo.register(merged);

    // 4) piloter les signaux
    this._presetId.set(merged.id);
    if (payload?.mode) this._mode.set(payload.mode);
    if (payload?.density != null) this._density.set(payload.density);
  }

  // ========= Helpers =========

  private resolveEffectiveMode(pref: ThemeMode): 'light' | 'dark' {
    if (pref === 'light' || pref === 'dark') return pref;
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  /** Si rien dans le repo, on injecte un thème par défaut "tchalanet". */
  private ensureDefault(): TchTheme {
    const existing = this.repo.get('tchalanet');
    if (existing) return existing;

    const fallback: TchTheme = {
      id: 'tchalanet',
      label: 'Tchalanet',
      kind: 'custom',
      mode: 'light',
      density: 0,
      matClass: 'mat-indigo-pink',
      palette: {
        primary: '#134D9F',
        onPrimary: '#ffffff',
        surface: '#ffffff',
        onSurface: '#111111',
        outline: 'rgba(0,0,0,.16)',
        accent: '#D84C51',
        tertiary: '#D84C51',
        shape: { cornerRadius: 12 },
      },
      tokens: {
        headerBg: 'var(--mat-sys-color-primary)',
        headerFg: 'var(--mat-sys-color-on-primary)',
      },
    };
    this.repo.register(fallback);
    return fallback;
  }

  // ========= Persistence =========

  private persist() {
    try {
      localStorage.setItem(LS_MODE, this._mode());
      localStorage.setItem(LS_DENSE, String(this._density()));
      const id = this._presetId();
      if (id) localStorage.setItem(LS_PRESET, id);
    } catch {}
  }

  private restoreMode(): ThemeMode {
    try {
      const v = localStorage.getItem(LS_MODE) as ThemeMode | null;
      return v === 'light' || v === 'dark' || v === 'system' ? v : 'system';
    } catch {
      return 'system';
    }
  }

  private restorePresetId(): string | null {
    try {
      const v = localStorage.getItem(LS_PRESET);
      return v && v.trim().length ? v : null;
    } catch {
      return null;
    }
  }

  private restoreDensity(): 0 | -1 | -2 {
    try {
      const n = Number(localStorage.getItem(LS_DENSE));
      return n === -1 || n === -2 ? (n as -1 | -2) : 0;
    } catch {
      return 0;
    }
  }
}
