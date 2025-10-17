import { Component, HostListener, inject } from '@angular/core';
import { OverlayService } from './overlay.service';
import { MatIcon } from '@angular/material/icon';
import { AnalyticsService } from '@tchl/analytics';

@Component({
  standalone: true,
  selector: 'tchl-header-search-button',
  imports: [MatIcon],
  host: {
    class: 'tch-header-search-host',
  },
  template: `
    <button class="tch-search-pill" type="button" aria-label="Rechercher" (click)="open()">
      <mat-icon fontIcon="search" aria-hidden="true"></mat-icon>
      <span class="tch-search-pill__label">Rechercher</span>
    </button>
  `,
  styles: `
    /* make the custom element stretch inside flex parent */
    :host.tch-header-search-host {
      display: block;
      width: 100%;
      min-width: 0;
    }

    .tch-search-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      width: 100%;                    /* <— stretch */
      height: var(--pill, 44px);      /* uses your token */
      padding: 0 14px;
      border-radius: 999px;
      border: 1px solid var(--tch-border, #dcdde3);
      background: #fff;
      color: #111;
      box-shadow: 0 2px 8px rgba(16,24,40,.10);
      text-align: left;
    }

    .tch-search-pill:focus-visible {
      outline: 2px solid #4c8bf7;
      outline-offset: 2px;
    }

    .tch-search-pill__label { font-size: 16px; }

    @media (min-width: 600px) {
      .tch-search-pill { height: var(--pill, 46px); }
    }
  `,
})
export class TchHeaderSearchComponent {
  private overlay = inject(OverlayService);
  private analyticsService = inject(AnalyticsService);

  open() {
    this.overlay.show();
    this.analyticsService.pageView('open_search_from_button');
  }

  @HostListener('window:keydown', ['$event'])
  onShortcut(e: KeyboardEvent) {
    const inField = /input|textarea|select/i.test((e.target as HTMLElement).tagName);
    const cmdK = (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k';
    const slash = !e.metaKey && !e.ctrlKey && e.key === '/';
    if (!inField && (cmdK || slash)) {
      e.preventDefault();
      this.open();
      this.analyticsService.pageView(cmdK ? 'open_search_cmdk' : 'open_search_via_slash');
    }
  }
}
