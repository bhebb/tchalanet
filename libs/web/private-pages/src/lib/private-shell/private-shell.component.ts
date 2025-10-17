import { ChangeDetectionStrategy, Component, computed, inject, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectPage } from '@tchl/data-access/page';
import { ShellComponent } from '@tchl/web/shell';
import { FooterComponent, HeaderComponent, SidebarNavComponent } from '@tchl/ui/layout';
import { Brand } from '@tchl/types';

@Component({
  selector: 'tchl-private-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    ShellComponent,
    HeaderComponent,
    SidebarNavComponent,
    FooterComponent,
  ],
  template: `
    <tchl-shell [hasSidebar]="true">
      <tchl-header
        shell-header
        mode="private"
        [brand]="brand()"
        [navigation]="links()"
        [user]="user()"
        (menu)="onToggleSideNav()"
        (toggleTheme)="onToggleTheme()"
        (changeLang)="onChangeLang($event)"
      />

      <!-- sidenav projetée -->
      <tchl-sidebar-nav sidenav [links]="links()" [features]="features()"></tchl-sidebar-nav>

      <!-- contenu principal -->
      <router-outlet />
      <tchl-footer shell-footer [properties]="page()?.footer?.properties!" /></tchl-shell>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivateShellComponent {
  private store = inject(Store);
  page = this.store.selectSignal(selectPage);
  links = computed(() => this.page()?.nav?.sidenav ?? []);
  features = computed(() => this.page()?.features ?? []);
  brand = computed(() => this.page()?.header?.properties?.brand);
  user = computed(() => ({ avatarUrl: '' })); // adapte

  onToggleSideNav() {
    /* ouvrir/fermer ta sidenav (si matérialisée ailleurs) */
  }
  onToggleTheme() {
    /* toggle dark/light */
  }
  onChangeLang(l: string) {
    /* i18n */
  }
}
