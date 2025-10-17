import { AfterViewInit, Component, inject, OnDestroy } from '@angular/core';
import instantsearch, { SearchClient } from 'instantsearch.js';
import { instantMeiliSearch } from '@meilisearch/instant-meilisearch';
import {
  clearRefinements,
  configure,
  infiniteHits,
  refinementList,
  searchBox,
  sortBy,
} from 'instantsearch.js/es/widgets';
import 'instantsearch.css/themes/satellite-min.css';
import { environment } from '@tchl/config';
import { Router } from '@angular/router';
import { AnalyticsService } from '@tchl/analytics';

type Hit = {
  id: string;
  title: string;
  body?: string;
  url?: string;
  tenantId: string;
  lang: string;
  published_at?: string;
  __position?: number; // injecté par InstantSearch
};

@Component({
  standalone: true,
  selector: 'tchl-search-results',
  template: `
    <div class="results">
      <header class="top">
        <div id="searchbox"></div>
        <button class="filtersBtn" (click)="toggleFilters()">
          Filtres <span *ngIf="filtersCount">{{ filtersCount }}</span>
        </button>
      </header>

      <aside class="drawer" [class.open]="filtersOpen" aria-label="Filtres">
        <div id="clear"></div>
        <h4>Langue</h4>
        <div id="facet-lang"></div>
        <h4>Tags</h4>
        <div id="facet-tags"></div>
        <button class="apply" (click)="toggleFilters()">Appliquer</button>
      </aside>

      <section class="content">
        <div id="sort"></div>
        <div id="hits" class="hits"></div>
      </section>
    </div>
  `,
  styles: [
    `
      .results {
        padding: 0.75rem;
      }
      .top {
        position: sticky;
        top: 0;
        background: #fff;
        padding: 0.5rem;
        border-bottom: 1px solid #eee;
        display: flex;
        gap: 0.5rem;
        align-items: center;
        z-index: 5;
      }
      .filtersBtn {
        margin-left: auto;
        border: 1px solid #ddd;
        border-radius: 10px;
        padding: 0.4rem 0.7rem;
        background: #fafafa;
      }

      .drawer {
        position: fixed;
        inset: 0 0 0 auto;
        width: 85%;
        max-width: 360px;
        background: #fff;
        transform: translateX(100%);
        transition: 0.2s;
        z-index: 10;
        padding: 1rem;
        box-shadow: -8px 0 20px rgba(0, 0, 0, 0.08);
      }
      .drawer.open {
        transform: none;
      }
      .apply {
        position: fixed;
        right: 1rem;
        bottom: 1rem;
        border: 1px solid #ddd;
        border-radius: 999px;
        padding: 0.6rem 1rem;
        background: #fff;
      }

      .hits {
        display: grid;
        grid-template-columns: 1fr;
        gap: 0.75rem;
      }
      @media (min-width: 600px) {
        .hits {
          grid-template-columns: 1fr 1fr;
        }
      }
      @media (min-width: 1024px) {
        .hits {
          grid-template-columns: 1fr 1fr 1fr;
        }
      }

      .card {
        border: 1px solid #eee;
        border-radius: 12px;
        padding: 0.75rem;
        background: #fff;
        box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
      }
      .card h3 {
        margin: 0 0 0.25rem;
        font-size: 16px;
      }
      .snippet {
        color: #555;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      .card mark {
        background: transparent;
        color: #1a73e8;
        font-weight: 600;
      }
      .meta {
        font-size: 12px;
        color: #666;
        margin-top: 0.25rem;
      }
    `,
  ],
})
export class SearchResultsPage implements AfterViewInit, OnDestroy {
  private router = inject(Router);
  private analyticsService = inject(AnalyticsService);

  private search!: ReturnType<typeof instantsearch>;
  filtersOpen = false;
  filtersCount = 0;

  toggleFilters() {
    this.filtersOpen = !this.filtersOpen;
  }

  ngAfterViewInit() {
    const { searchClient } = instantMeiliSearch(environment.meiliHost, environment.meiliSearchKey);

    this.search = instantsearch({
      indexName: environment.indexName,
      searchClient: searchClient as unknown as SearchClient,
      routing: true, // garde ?q= dans l’URL
    });

    // Widgets (adaptés à tes champs)
    this.search.addWidgets([
      configure({
        hitsPerPage: 12,
        filters: `tenantId = ${environment.tenant ?? 'public'} AND lang = ${
          environment.lang ?? 'fr'
        }`,
        attributesToHighlight: ['title'],
        attributesToSnippet: ['body:30'],
        snippetEllipsisText: '…',
      }),
      searchBox({
        container: '#searchbox',
        placeholder: 'Rechercher…',
        showReset: true,
        showSubmit: true,
      }),
      sortBy({
        container: '#sort',
        items: [
          { label: 'Plus récents', value: `${environment.indexName}:published_at:desc` },
          { label: 'Moins récents', value: `${environment.indexName}:published_at:asc` },
        ],
      }),
      clearRefinements({ container: '#clear', templates: { resetLabel: 'Effacer les filtres' } }),
      refinementList({ container: '#facet-lang', attribute: 'lang', searchable: true }),
      refinementList({ container: '#facet-tags', attribute: 'tags', searchable: true }),

      infiniteHits<Hit>({
        container: '#hits',
        templates: {
          item(hit, { html, components }) {
            const date = hit.published_at ? new Date(hit.published_at).toLocaleDateString() : '';
            return html`
              <article class="card" data-hit-id=${hit.id} data-hit-pos=${hit.__position || 0}>
                <a href=${hit.url || '#'} class="hit-link">
                  <h3>${components.Highlight({ hit, attribute: 'title' })}</h3>
                </a>
                <p class="snippet">${components.Snippet({ hit, attribute: 'body' })}</p>
                <div class="meta">${hit.lang} · ${date}</div>
              </article>
            `;
          },
          empty(params: any) {
            const { html, state } = params;
            const q = state?.query || '';
            return html`<div class="ais-Hits-empty">
              Aucun résultat pour “${q}”.
            </div>`;
          },
        },
      }),
    ]);

    // Compteur filtres actifs + analytics
    this.search.on('render', () => {
      const helper = (this.search as any).helper;
      const state = helper?.state || {};
      const total = (this.search as any).results?.nbHits ?? 0;
      const q = state.query || '';

      const nb =
        (state.disjunctiveFacetsRefinements?.lang?.length || 0) +
        (state.disjunctiveFacetsRefinements?.tags?.length || 0);
      this.filtersCount = nb;

      if (q && total === 0)
        this.analyticsService.searchNoResults(q, {
          tenantId: environment.tenant ?? 'public',
          lang: environment.lang ?? 'fr',
        });
      else if (q)
        this.analyticsService.searchViewSuggestions(q, {
          tenantId: environment.tenant ?? 'public',
          lang: environment.lang ?? 'fr',
        });
    });

    // Navigation SPA au clic des résultats (Angular Router)
    const onDocClick = (e: Event) => {
      const a = (e.target as HTMLElement).closest('#hits .hit-link') as HTMLAnchorElement | null;
      if (!a) return;
      e.preventDefault();
      const card = a.closest('.card') as HTMLElement | null;
      const id = card?.getAttribute('data-hit-id') || a.href;
      const pos = Number(card?.getAttribute('data-hit-pos') || '0');

      this.analyticsService.selectSearchResult(id, pos, {
        tenantId: environment.tenant ?? 'public',
        lang: environment.lang ?? 'fr',
      });

      // navigation SPA si url interne
      const url = a.getAttribute('href') || '/';
      if (url.startsWith('/')) this.router.navigateByUrl(url);
      else window.location.assign(url);
    };
    document.addEventListener('click', onDocClick, { capture: true });

    this.search.start();

    // cleanup
    this.ngOnDestroy = () => {
      this.search?.dispose();
      document.removeEventListener('click', onDocClick, { capture: true } as any);
    };
  }

  ngOnDestroy() {}
}
