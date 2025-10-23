import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { PageFacade } from '@tchl/facades';
import { SearchOverlayComponent } from '@tchl/search';
import { FooterComponent, OverlayNavComponent,PublicHeaderComponent } from '@tchl/ui/layout';
import { ShellComponent } from '@tchl/web/shell';

@Component({
  selector: 'tchl-public-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    ShellComponent,
    FooterComponent,
    OverlayNavComponent,
    SearchOverlayComponent,
    PublicHeaderComponent,
  ],
  template: `
    <tchl-shell [hasSidebar]="false">
      <tchl-public-header
        shell-header
        mode="public"
        [properties]="headerProps()!"
        [flags]="flags() || []"
        (menuClick)="openOverlay()"
      />
      <!-- MAIN CONTENT -->
      <router-outlet />

      <tchl-footer shell-footer [properties]="page()?.footer?.properties!" />
    </tchl-shell>
    <tchl-search-overlay></tchl-search-overlay>
    <tchl-overlay-nav
      [open]="overlayOpen()"
      [items]="page()?.header?.properties?.navigation || []"
      (requestClose)="closeOverlay()"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicShellComponent implements OnInit {
  private readonly pageFacade = inject(PageFacade);
  page = this.pageFacade.page;

  flags = computed(() => this.page()?.flags ?? []);

  headerProps = computed(() => {
    const p = this.page();
    return p?.header?.properties;
  });


  private readonly _overlayOpen = signal(false);
  overlayOpen = computed(() => this._overlayOpen());

  ngOnInit() {
    this.pageFacade.load('home-public', 'default');
  }

  openOverlay() {
    this._overlayOpen.set(true);
  }

  closeOverlay() {
    this._overlayOpen.set(false);
  }
}
