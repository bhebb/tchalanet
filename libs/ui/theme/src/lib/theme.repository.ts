import { TchTheme } from '@tchl/types';
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeRepository {
  private cache = new Map<string, TchTheme>();
  private _ready = signal(false);
  private _version = signal(0); // bump à chaque register

  ready = this._ready.asReadonly();
  version = this._version.asReadonly();

  registerMany(list: TchTheme[]) {
    for (const t of list) this.register(t);
    this._ready.set(true);
  }

  register(theme: TchTheme) {
    this.cache.set(this.norm(theme.id), theme);
    this._version.update(v => v + 1);
  }

  get(id: string | null | undefined): TchTheme | null {
    if (!id) return null;
    return this.cache.get(this.norm(id)) ?? null;
  }

  has(id: string | null | undefined): boolean {
    return !!id && this.cache.has(this.norm(id));
  }

  private norm(id: string) {
    return id.trim().toLowerCase();
  }
}
