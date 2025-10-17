import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TranslationOverrideApi {
  private readonly http = inject(HttpClient);

  getOverrides(context: string, lang: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(
      `v1/i18n-overrides?context=${context}&lang=${lang}`,
    );
  }
}
