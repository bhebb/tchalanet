import { Injectable } from '@angular/core';
import { TchTheme } from '@tchl/types';

@Injectable({ providedIn: 'root' })
export class ThemeAdapter {
  // merge un preset (du repo) + payload backend (overrides) -> theme prêt pour dom
  merge(preset: TchTheme, payload?: TchTheme): TchTheme {
    if (!payload) return preset;
    return {
      ...preset,
      mode: payload.mode ?? preset.mode,
      density: payload.density ?? preset.density,
      cssVars: { ...preset.cssVars, ...(payload.cssVars ?? {}) },
      tokens: { ...preset.tokens, ...(payload.tokens ?? {}) },
      palette: { ...preset.palette, ...payload.palette },
    };
  }
}
