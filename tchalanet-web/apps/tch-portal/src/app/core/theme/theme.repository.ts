import { Injectable, signal } from '@angular/core';

import { ThemePreset } from '../../shared/types';
import { defaultThemePresetId, fallbackThemePresets } from './theme-presets';

@Injectable({ providedIn: 'root' })
export class ThemeRepository {
  private readonly presets = new Map<string, ThemePreset>();
  private readonly readyState = signal(false);

  readonly ready = this.readyState.asReadonly();

  constructor() {
    fallbackThemePresets.forEach((preset) => this.presets.set(preset.id, preset));
    this.readyState.set(true);
  }

  defaultPreset(): ThemePreset {
    return this.get(defaultThemePresetId) ?? fallbackThemePresets[0];
  }

  get(id: string): ThemePreset | null {
    return this.presets.get(id) ?? null;
  }

  has(id: string): boolean {
    return this.presets.has(id);
  }

  list(): readonly ThemePreset[] {
    return Array.from(this.presets.values());
  }

  upsert(preset: ThemePreset): void {
    this.presets.set(preset.id, preset);
  }
}
