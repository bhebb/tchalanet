import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TCH_PUBLIC_ASSETS } from '@tch/shared-assets';

export type PublicMarkdownFile = 'privacy' | 'terms';

@Injectable({ providedIn: 'root' })
export class PublicMarkdownContentService {
  private readonly http = inject(HttpClient);

  load(file: PublicMarkdownFile): Observable<string> {
    return this.http.get(`${TCH_PUBLIC_ASSETS.pagesPath}/${file}.md`, {
      responseType: 'text',
    });
  }
}
