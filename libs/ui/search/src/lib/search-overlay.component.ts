import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import { environment } from '@tchl/config';
import { Hit, MeiliSearch } from 'meilisearch';
import { AnalyticsService } from '@tchl/analytics';
import { OverlayService } from './overlay.service';
import { autocomplete, AutocompleteApi } from '@algolia/autocomplete-js';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'tchl-search-overlay',
  imports: [CommonModule],
  encapsulation: ViewEncapsulation.None, // 👈 styles non encapsulés
  template: `
    <div class="tchl-search-overlay" (click)="close()">
      <div class="sheet" role="dialog" aria-label="Recherche" (click)="$event.stopPropagation()">
        <div class="bar">
          <div #root></div>
          <a class="clear" href (click)="onClear($event)" [class.hidden]="!_hasQuery">Clear</a>
          <button class="close" type="button" aria-label="Fermer" (click)="close()">×</button>
        </div>
        <div id="aa-panel" class="panel"></div>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: contents;
      }

      .tchl-search-overlay {
        position: fixed;
        inset: 0;
        z-index: 3000;
        background: rgba(17, 24, 39, 0.45);
        backdrop-filter: blur(2px);
        display: none; /* ⬅️ au lieu de "contents" */
        pointer-events: none; /* évite de bloquer la page quand fermé */
      }

      .tchl-search-overlay.open {
        display: block;
        pointer-events: auto;
      }

      .sheet {
        width: min(920px, calc(100vw - 24px));
        margin: 6vh auto 0;
        background: #fff;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 24px 56px rgba(16, 24, 40, 0.18);
      }

      .bar {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 12px 14px;
        border-bottom: 1px solid #eef2f6;
      }

      .panel {
        max-height: min(72vh, 720px);
        overflow: auto;
        padding: 8px 12px;
      }

      .clear {
        margin-left: auto;
        font-size: 14px;
        color: #2563eb;
        text-decoration: none;
      }

      .clear.hidden {
        visibility: hidden;
      }

      .close {
        font-size: 22px;
        border: 0;
        background: transparent;
        padding: 4px 8px;
        cursor: pointer;
        color: #475467;
      }

      /* lisibilité */
      .aa-Form {
        min-width: 0;
        flex: 1;
      }

      .aa-Item,
      .aa-Item * {
        color: #111;
        -webkit-text-fill-color: #111;
      }

      .aa-Item mark {
        background: transparent;
        color: #1a73e8;
        font-weight: 600;
      }

      .aa-ItemLink {
        display: block;
        padding: 10px 12px;
        border-bottom: 1px solid #eee;
        text-decoration: none;
        color: inherit;
      }

      .aa-ItemLink:hover .aa-ItemTitle,
      .aa-ItemLink:focus .aa-ItemTitle {
        text-decoration: underline;
      }

      .aa-ItemTitle {
        font-weight: 600;
        margin: 0 0 4px 0;
      }

      .aa-ItemSubtitle {
        color: #444;
        margin: 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchOverlayComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('root', { static: true }) rootRef!: ElementRef<HTMLElement>;

  private overlaySvc = inject(OverlayService);
  private analytics = inject(AnalyticsService, { optional: true });

  private meili = new MeiliSearch({
    host: environment.meiliHost,
    apiKey: environment.meiliSearchKey,
  });
  private api!: AutocompleteApi<Hit>;

  private host = inject(ElementRef<HTMLElement>);
  private r = inject(Renderer2);
  private anchor?: Comment;

  private _rootEl!: HTMLElement;
  private _containerEl!: HTMLElement;
  private _isOpen = false;
  _hasQuery = false;

  ngOnInit() {
    this.anchor = document.createComment('tchl-search-overlay-anchor');
    const parent = this.host.nativeElement.parentNode;
    if (parent) this.r.insertBefore(parent, this.anchor, this.host.nativeElement);
    this.r.appendChild(document.body, this.host.nativeElement);
  }

  ngAfterViewInit(): void {
    // refs utiles
    this._rootEl = this.rootRef.nativeElement;
    this._containerEl = this._rootEl.closest('.tchl-search-overlay') as HTMLElement;

    // expose "ouvrir/fermer" au header
    this.overlaySvc.register({
      show: () => this.open(),
      close: () => this.close(),
    });

    const meili = this.meili;
    this.api = autocomplete<Hit>({
      container: this._rootEl,
      placeholder: 'Rechercher…',
      openOnFocus: true,
      detachedMediaQuery: 'none',
      panelContainer: '#aa-panel',
      defaultActiveItemId: 0, // surligne le 1er résultat (meilleure “cue” clavier)
      translations: { detachedCancelButtonText: 'Annuler' },

      getSources: ({ query }: any) => [
        {
          sourceId: 'tch_content',
          async getItems() {
            const q = (query ?? '').trim();
            if (q.length < 1) return [];
            const res = await meili.index(environment.indexName).search<Hit>(q, {
              limit: 8,
              filter: 'tenantId = "public" AND lang = "fr"',
              attributesToHighlight: ['title', 'body'],
              attributesToCrop: ['body:100'],
              cropMarker: '…',
            });
            return res.hits || [];
          },
          templates: {
            item({ item, html }: any) {
              const title = item?.title ?? '(sans titre)';
              const body = String(item?.body ?? '').slice(0, 140);
              const href = item?.url || `/results?q=${encodeURIComponent(title)}`;
              const path = (item?.url || '')
                .replace(/^https?:\/\/([^/]+)\//, '$1 › ')
                .replace(/\/$/, '');
              const snippet = item?._formatted?.body ?? '';

              return html`
                <a class="tch-hit" href="${href}">
                  <span class="tch-hit__icon" aria-hidden="true">🡥</span>
                  <span class="tch-hit__main">
                    <span class="tch-hit__title">${title}</span>
                    <span class="tch-hit__subtitle"> ${html`${snippet}`} </span>
                    ${path ? html`<span class="tch-hit__meta">${path}</span>` : ''}
                    <!--        ^^^^^ use html tag here -->
                  </span>
                  <span class="tch-hit__chev" aria-hidden="true">›</span>
                </a>
              `;
            },
            noResults({ state, html }: any) {
              return html`<div class="aa-NoResults">Aucun résultat pour « ${state.query} »</div>`;
            },
          } as any,
          onSelect({ item, state }: any) {
            if (item?.id === 'error') return;
            if (item?.url) window.location.assign(item.url);
            else window.location.assign(`/results?q=${encodeURIComponent(state.query)}`);
          },
        },
      ],

      onStateChange: ({ state }: any) => {
        this._isOpen = !!state.isOpen;
        this._hasQuery = !!state.query?.trim();
        // toggle visibilité overlay
        this._containerEl.classList.toggle('open', this._isOpen);
        // décor global
        if (this._isOpen) document.documentElement.classList.add('search-open');
        else document.documentElement.classList.remove('search-open');
        // analytics
        if (this._isOpen) this.analytics?.pageView('view_suggestions');
      },
    } as any);

    // si show() a été appelé avant register(), register() a déjà rejoué open().
    // on s’assure aussi de focus l’input à l’ouverture :
    // (géré dans open())
  }

  private focusInput() {
    queueMicrotask(() => {
      const input = this._containerEl.querySelector<HTMLInputElement>('.aa-Input');
      input?.focus();
    });
  }

  open() {
    this.api?.setIsOpen(true);
    this.focusInput();
  }

  close() {
    this.api?.setIsOpen(false);
  }

  onClear(ev: Event) {
    ev.preventDefault();
    this.api?.setQuery('');
    this.api?.refresh();
    this.focusInput();
  }

  ngOnDestroy(): void {
    this.api?.destroy();
    document.documentElement.classList.remove('search-open');
    if (this.anchor?.parentNode) {
      this.r.insertBefore(this.anchor.parentNode, this.host.nativeElement, this.anchor);
      this.anchor.remove();
    }
  }
}
