import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { PageFacade } from '@tchl/facades';
import { ShellComponent } from '@tchl/web/shell';
import { FooterComponent, HeaderComponent } from '@tchl/ui/layout';
import { OverlayService, SearchOverlayComponent } from '@tchl/search';

@Component({
  selector: 'public-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    ShellComponent,
    HeaderComponent,
    FooterComponent,
    SearchOverlayComponent,
  ],
  template: `
    @if (page(); as p) {
      <tchl-shell>
        <tchl-header
          shell-header
          mode="public"
          [brand]="p.header.properties?.brand"
          [navigation]="p.header.properties?.navigation!"
          [cta]="p.header.properties?.cta"
          [showLang]="true"
          [showTheme]="true"
        />
        <router-outlet />
        <tchl-footer shell-footer [properties]="p.footer.properties!" />
      </tchl-shell>
      <tchl-search-overlay></tchl-search-overlay>
    } @else {
      <!-- loading / error selon ton besoin -->
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicShellComponent implements OnInit {
  private readonly pageFacade = inject(PageFacade);
  page = this.pageFacade.page;
  overlayService = inject(OverlayService);

  ngOnInit() {
    this.pageFacade.load('home-public', 'default');
  }
}
