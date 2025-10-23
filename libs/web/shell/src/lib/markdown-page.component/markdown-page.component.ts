import { MarkdownModule, MARKED_OPTIONS, MarkedOptions } from 'ngx-markdown';

import { combineLatest, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, startWith, switchMap } from 'rxjs/operators';

import { CommonModule, DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';

import { selectCurrentLang } from '@tchl/data-access/i18n';
import { I18nFacade } from '@tchl/facades';

type FrontMatter = {
  title?: string;
  description?: string;
  noindex?: boolean; // root-level support
  updated_at?: string; // affichage éventuel
  route?: string;
  seo?: {
    canonical?: string;
    noindex?: boolean; // nested support
    image?: string;
  };
};

@Component({
  standalone: true,
  selector: 'tch-markdown-page',
  imports: [CommonModule, MarkdownModule],
  providers: [
    {
      provide: MARKED_OPTIONS,
      useValue: <MarkedOptions>{
        gfm: true,          // tables, task lists, autolinks…
        breaks: false,
        headerIds: true,    // ancres automatiques sur titres
        mangle: false,      // évite d’échapper les emails
      },
    },
  ],
  template: `
    <section class="h-container page">
      @if (front().title; as t) { <h1>{{ t }}</h1> }
      <markdown [data]="body()"></markdown>
      @if (error(); as e) { <p class="muted">{{ e }}</p> }
    </section>
  `,
  styles: [`
    :host {
      --accent: #D84C51;
      --text: #222;
      --muted: #6b7280;
    }

    .page {
      padding-block: clamp(20px, 4vw, 40px);
    }

    .h-container {
      max-width: var(--tch-page-max, 1120px);
      margin: 0 auto;
      padding-inline: var(--tch-page-gutter, 24px);
    }

    /* TITRES + SOUS-TITRES (plus sobres) */
    markdown h1, .page > h1 {
      margin: 0 0 .75rem 0;
      font-size: clamp(24px, 3.2vw, 32px);
      font-weight: 700;
      letter-spacing: -0.02em;
      color: var(--text);
    }

    markdown h2 {
      margin: 2rem 0 .5rem;
      font-size: clamp(18px, 2.2vw, 22px);
      font-weight: 700;
    }

    markdown h3 {
      margin: 1.25rem 0 .25rem;
      font-size: clamp(16px, 2vw, 18px);
      font-weight: 600;
      color: var(--text);
    }

    /* Petit soulignement discret sous H2 uniquement */
    markdown h2::after {
      content: "";
      display: block;
      width: 40px;
      height: 3px;
      margin-top: .4rem;
      border-radius: 2px;
      background: var(--accent);
    }

    /* PARAGRAPHES & LISTES */
    markdown p {
      line-height: 1.65;
      margin: .6rem 0 1rem;
    }

    markdown ul, markdown ol {
      padding-left: 1.1rem;
    }

    markdown li {
      margin: .25rem 0;
    }

    /* CALLOUTS (blockquote) */
    markdown blockquote {
      margin: 1rem 0;
      padding: .9rem 1rem;
      border-left: 4px solid var(--accent);
      background: #fff5f6;
      border-radius: 8px;
      color: #3a3a3a;
    }

    markdown blockquote p {
      margin: 0;
    }

    /* TABLES — style léger + scroll propre */
    markdown table {
      border-collapse: separate;
      border-spacing: 0;
      width: 100%;
      font-size: .95rem;
      overflow: hidden;
      border-radius: 12px;
      box-shadow: 0 1px 0 rgba(0, 0, 0, .06);
    }

    markdown thead th {
      text-align: left;
      font-weight: 700;
      padding: .75rem .8rem;
      background: #f6f7fb;
      border-bottom: 1px solid #ececf3;
      white-space: nowrap;
    }

    markdown tbody td {
      padding: .65rem .8rem;
      border-bottom: 1px solid #f0f1f6;
    }

    markdown tbody tr:last-child td {
      border-bottom: 0;
    }

    /* zébrage doux */
    markdown tbody tr:nth-child(2n) td {
      background: #fcfcfe;
    }

    /* WRAPPER SCROLL (déjà présent dans tes fichiers) */
    markdown .md-scroll {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      margin: .5rem 0 1rem;
      padding-bottom: .25rem;
    }

    /* ALTERNATIVES “SANS TABLEAU” — grilles de cartes & listes KV */
    markdown .grid.g2 {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 12px;
      margin: .8rem 0 1.2rem;
    }

    markdown .card {
      border: 1px solid #ececf3;
      border-radius: 12px;
      padding: .9rem;
      background: #fff;
    }

    markdown .card .k {
      font-weight: 600;
      color: #111;
    }

    markdown .card .v {
      color: #444;
    }

    /* Petites images dans le contenu */
    markdown ::ng-deep img {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkdownPageComponent {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private i18n = inject(I18nFacade);
  private title = inject(Title);
  private meta = inject(Meta);
  private store = inject(Store);
  private document = inject(DOCUMENT);

  readonly front = signal<FrontMatter>({});
  readonly body = signal<string>('');
  readonly error = signal<string | null>(null);

  constructor() {
    // 1) slug depuis data
    const slug$ = this.route.data.pipe(
      map(d => d['slug'] as string),
      distinctUntilChanged(),
    );

    // 2) langue actuelle
    const lang$ = this.store.select(selectCurrentLang).pipe(
      map(code => (code?.trim() || this.i18n.current || 'fr')),
      startWith(this.i18n.current || 'fr'),
      distinctUntilChanged(),
    );

    // 3) charge le markdown
    combineLatest([slug$, lang$]).pipe(
      takeUntilDestroyed(),
      switchMap(([slug, lang]) => {
        // NOTE: on a normalisé sur assets/content/{lang}/{slug}.md
        const url = `assets/content/${lang}/${slug}.md`;
        return this.http.get(url, { responseType: 'text' }).pipe(
          map(src => ({ slug, lang, src })),
          catchError(() => of({ slug, lang, src: null as string | null })),
        );
      })
    ).subscribe(({ src }) => {
      if (!src) {
        this.front.set({});
        this.body.set('');
        this.error.set(this.i18n.instant('seo.placeholder_coming_soon') || 'Contenu à venir.');
        const t = this.i18n.instant('seo.default_title') || 'Tchalanet';
        const d = this.i18n.instant('seo.default_desc') || '';
        this.applySEO({ title: t, description: d, noindex: true, canonical: this.currentPath() });
        return;
      }

      const { fm, md } = this.parseFrontMatter(src);
      this.front.set(fm);
      const cleaned = this.stripLeadingH1(md, fm.title);
      this.body.set(cleaned);
      this.error.set(null);

      // Support root noindex OU seo.noindex (priorité au nested)
      const noindex = fm.seo?.noindex ?? fm.noindex ?? false;
      const canonical = fm.seo?.canonical || fm.route || this.currentPath();

      this.applySEO({
        title: fm.title || 'Tchalanet',
        description: fm.description || '',
        noindex,
        canonical,
      });
    });
  }

  /** Écrit title/description/robots + <link rel="canonical"> */
  private applySEO(opts: { title: string; description: string; noindex: boolean; canonical: string }) {
    const { title, description, noindex, canonical } = opts;
    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ name: 'robots', content: noindex ? 'noindex, nofollow' : 'index, follow' });

    // canonical (supprime l’ancienne si présente)
    const head = this.document.head as HTMLHeadElement;
    const prev = head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (prev) head.removeChild(prev);
    const link = this.document.createElement('link');
    link.setAttribute('rel', 'canonical');
    link.setAttribute('href', canonical.startsWith('http') ? canonical : this.absoluteUrl(canonical));
    head.appendChild(link);
  }

  private currentPath(): string {
    // renvoie l'URL absolue de la page courante (sans query/hash)
    const { origin, pathname } = window.location;
    return `${origin}${pathname}`;
  }

  private absoluteUrl(pathOrUrl: string): string {
    if (pathOrUrl.startsWith('http')) return pathOrUrl;
    const base = window.location.origin.replace(/\/$/, '');
    const p = pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`;
    return `${base}${p}`;
  }

  /** Parseur front-matter YAML minimal : clés à plat + bloc `seo:` (noindex, canonical, image). */
  private parseFrontMatter(src: string): { fm: FrontMatter; md: string } {
    if (src.startsWith('---')) {
      const end = src.indexOf('\n---', 3);
      if (end > 0) {
        const raw = src.slice(3, end).trim();
        const md = src.slice(end + 4).trim();
        const fm: FrontMatter = {};
        let inSeo = false;

        raw.split('\n').forEach(line => {
          const l = line.replace(/\r$/, '');

          // entrée / sortie du bloc seo:
          if (/^seo:\s*$/.test(l)) {
            inSeo = true;
            fm.seo ||= {};
            return;
          }
          if (inSeo && /^\w/.test(l)) inSeo = false;

          // lignes key: value (root ou seo)
          const m = l.match(/^\s*([A-Za-z_][\w\-]*):\s*(.*)\s*$/);
          if (!m) return;
          const key = m[1]; let valRaw = m[2];

          // nettoie quotes
          valRaw = valRaw.replace(/^"(.*)"$/, '$1').replace(/^'(.*)'$/, '$1');

          // booleans
          const val: any = (valRaw === 'true') ? true : (valRaw === 'false') ? false : valRaw;

          // affectation
          if (inSeo && fm.seo) {
            if (key === 'noindex') fm.seo.noindex = val === true || val === 'true';
            else if (key === 'canonical') fm.seo.canonical = String(val);
            else if (key === 'image') fm.seo.image = String(val);
          } else {
            if (key === 'noindex') (fm as any).noindex = val === true || val === 'true';
            else (fm as any)[key] = val;
          }
        });

        return { fm, md };
      }
    }
    return { fm: {}, md: src };
  }

  // Retire le premier H1 s’il duplique front.title
  private stripLeadingH1(md: string, title?: string): string {
    if (!title) return md;
    const [first, ...rest] = md.split('\n');
    const h = first?.trim();
    if (h?.startsWith('#')) {
      const h1 = h.replace(/^#+\s*/, '').trim().toLowerCase();
      if (h1 === title.trim().toLowerCase()) return rest.join('\n').trimStart();
    }
    return md;
  }
}
