import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Data } from '@angular/router';
import { marked } from 'marked';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';

import {
  PublicMarkdownContentService,
  PublicMarkdownFile,
} from './data-access/public-markdown-content.service';

type LoadState = 'loading' | 'loaded' | 'error';

@Component({
  selector: 'tch-public-markdown-page',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-markdown.page.html',
  styleUrls: ['./public-markdown.page.scss'],
})
export class PublicMarkdownPage {
  private readonly content = inject(PublicMarkdownContentService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly route = inject(ActivatedRoute);

  private readonly routeData = toSignal(this.route.data, {
    initialValue: this.route.snapshot.data,
  });

  readonly file = computed(() => readFile(this.routeData()));
  readonly titleKey = computed(() => `public.pages.${this.file()}.title`);
  readonly eyebrowKey = computed(() => `public.pages.${this.file()}.eyebrow`);

  readonly state = signal<LoadState>('loading');

  readonly html = toSignal(
    this.route.data.pipe(
      map(d => readFile(d)),
      distinctUntilChanged(),
      switchMap(file => {
        this.state.set('loading');
        return this.content.load(file).pipe(
          map(md => {
            this.state.set('loaded');
            return this.sanitizer.bypassSecurityTrustHtml(
              marked.parse(md, { async: false }) as string,
            );
          }),
          catchError(() => {
            this.state.set('error');
            return of(null as SafeHtml | null);
          }),
        );
      }),
    ),
    { initialValue: null as SafeHtml | null },
  );
}

function readFile(data: Data): PublicMarkdownFile {
  const v = data['file'];
  return v === 'privacy' || v === 'terms' ? v : 'privacy';
}
