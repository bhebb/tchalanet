// theme-assets.loader.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { TchTheme } from '@tchl/types';

type ThemeIndex = {
  version: number;
  themes: { id: string; url: string }[];
};

@Injectable({ providedIn: 'root' })
export class ThemeAssetsLoader {
  private http = inject(HttpClient);

  async listIds(): Promise<ThemeIndex> {
    return firstValueFrom(this.http.get<ThemeIndex>('/assets/themes/index.json'));
  }

  async fetch(url: string): Promise<TchTheme> {
    return firstValueFrom(this.http.get<TchTheme>(url));
  }
}
