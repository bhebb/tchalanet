import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { PageFacade } from '@tchl/facades';
import { ShellComponent } from '@tchl/web/shell';
import { FooterComponent, HeaderComponent } from '@tchl/ui/layout';
import { SearchOverlayComponent } from '@tchl/search';
import { BreadcrumbComponent } from '@tchl/breadcrumb';

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
    BreadcrumbComponent,
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
        [actions]="p.header.properties?.actions"
        [account]="p.header.properties?.account"
        [showLang]="true"
        [showTheme]="true"
      />
      <tchl-breadcrumb />
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

  ngOnInit() {
    this.pageFacade.load('home-public', 'default');
  }
}
