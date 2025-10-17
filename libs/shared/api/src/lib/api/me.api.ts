// core/api/me.api.ts
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class MeApi {
  private http = inject(HttpClient);
  getContext(tenantId: string, featureSetId: string) {
    return this.http.get<{
      features: string[];
      availableLangs: string[];
      i18nOverrides?: Record<string, string>;
      theme?: {
        primary?: string;
        accent?: string;
        surface?: string;
        onSurface?: string;
        radius?: string;
        spacing?: string;
        mode?: 'light' | 'dark';
      };
    }>('/v1/me/context', { params: { featureSetId } });
  }
}
