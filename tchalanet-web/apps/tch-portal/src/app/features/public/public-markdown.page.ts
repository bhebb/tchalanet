import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Data } from '@angular/router';
import { marked } from 'marked';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';



type MarkdownFile = 'privacy' | 'terms';
type LoadState = 'loading' | 'loaded' | 'error';

@Component({
  selector: 'tch-public-markdown-page',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="md-page">
      <header class="md-page__header">
        <p class="md-page__eyebrow">{{ eyebrowKey() | translate }}</p>
        <h1>{{ titleKey() | translate }}</h1>
      </header>

      @switch (state()) {
        @case ('loading') {
          <div class="md-page__loading" role="status" [attr.aria-label]="'public.markdown.loading' | translate">
            <span class="material-symbols-outlined md-page__spin" aria-hidden="true">progress_activity</span>
          </div>
        }
        @case ('error') {
          <div class="md-page__error" role="alert">
            <span class="material-symbols-outlined" aria-hidden="true">error_outline</span>
            <p>{{ 'public.markdown.error' | translate }}</p>
          </div>
        }
        @case ('loaded') {
          <article class="md-page__content" [innerHTML]="html()"></article>
        }
      }
    </div>
  `,
  styles: [
    `
      .md-page {
        display: grid;
        gap: 2rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 720px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 3rem) 0 5rem;
      }

      .md-page__header { display: grid; gap: 0.5rem; }

      .md-page h1, .md-page p { margin: 0; }

      .md-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .md-page h1 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        font-weight: 800;
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .md-page__loading {
        display: flex;
        justify-content: center;
        padding: 3rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .md-page__spin { animation: md-spin 1s linear infinite; }

      @keyframes md-spin { to { transform: rotate(360deg); } }

      .md-page__error {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        padding: 2rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-align: center;
      }

      .md-page__content {
        line-height: 1.7;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .md-page__content :is(h2, h3) {
        margin: 1.5rem 0 0.5rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 700;
      }

      .md-page__content h2 { font-size: var(--tch-font-size-title-md, 1.125rem); }
      .md-page__content h3 { font-size: 1rem; }

      .md-page__content p {
        margin: 0.75rem 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .md-page__content :is(ul, ol) {
        margin: 0.75rem 0;
        padding-left: 1.5rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .md-page__content li { margin: 0.375rem 0; }

      .md-page__content strong {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-weight: 700;
      }

      .md-page__content a { color: var(--tch-color-primary, var(--mat-sys-primary)); }

      .md-page__content hr {
        border: none;
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        margin: 1.5rem 0;
      }

      @media (min-width: 760px) {
        .md-page h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }
      }
    `,
  ],
})
export class PublicMarkdownPage {
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly route = inject(ActivatedRoute);

  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });

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
        return this.http
          .get(`/assets/public/pages/${file}.md`, { responseType: 'text' })
          .pipe(
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

function readFile(data: Data): MarkdownFile {
  const v = data['file'];
  return v === 'privacy' || v === 'terms' ? v : 'privacy';
}
