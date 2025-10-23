import { Injectable, signal } from '@angular/core';

import { TchTheme } from './theme.types';
import { THEME_PRESETS } from './theme-presets.registry';

@Injectable({ providedIn: 'root' })
export class ThemeRepository {
  private readonly _map = new Map<string, TchTheme>();
  private readonly _ready = signal(false);
  ready = this._ready.asReadonly();

  constructor() {
    // THEME_PRESETS est importé
    for (const preset of THEME_PRESETS) {
      this._map.set(preset.id, preset);
    }
    this._ready.set(true);
  }

  has(id: string): boolean {
    return this._map.has(id);
  }

  get(id: string): TchTheme | null {
    return this._map.get(id) ?? null;
  }

  list(): TchTheme[] {
    return Array.from(this._map.values());
  }

  /** pour le futur si on clone + modifie un tenant runtime */
  upsert(preset: TchTheme) {
    this._map.set(preset.id, preset);
  }
}
