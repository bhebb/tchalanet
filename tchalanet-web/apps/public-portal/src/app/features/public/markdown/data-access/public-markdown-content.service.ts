import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable, catchError } from 'rxjs';

import { TCH_PUBLIC_ASSETS } from '@tch/shared-assets';

export type PublicMarkdownFile = 'privacy' | 'terms' | 'data-deletion';

@Injectable({ providedIn: 'root' })
export class PublicMarkdownContentService {
  private readonly http = inject(HttpClient);
  private readonly translate = inject(TranslateService);

  load(file: PublicMarkdownFile): Observable<string> {
    const lang = this.translate.currentLang() || this.translate.fallbackLang() || 'ht';
    return this.http
      .get(`${TCH_PUBLIC_ASSETS.pagesPath}/${lang}/${file}.md`, { responseType: 'text' })
      .pipe(
        catchError(() =>
          this.http.get(`${TCH_PUBLIC_ASSETS.pagesPath}/${file}.md`, { responseType: 'text' }),
        ),
      );
  }
}
