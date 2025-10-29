import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { BreadcrumbComponent } from '@tchl/breadcrumb';
import { PageFacade } from '@tchl/facades';
import { FooterComponent, PrivateHeaderComponent, SidebarNavComponent } from '@tchl/ui/layout';
import { ShellComponent } from '@tchl/web/shell';

@Component({
  selector: 'tchl-private-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    ShellComponent,
    PrivateHeaderComponent,
    SidebarNavComponent,
    BreadcrumbComponent,
    FooterComponent,
  ],
  templateUrl: './private-shell.component.html',
  styleUrl: './private-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivateShellComponent {
  private readonly pageFacade = inject(PageFacade);
  page = this.pageFacade.page;

  flags = computed(() => this.page()?.flags ?? []);

  headerProps = computed(() => {
    const p = this.page();
    return p?.header?.properties;
  });

  sideLinks = computed(() => this.page()?.nav?.sidenav ?? []);

  userInfo = computed(() => {
    // TODO brancher ton auth réel
    return {
      displayName: 'Jane Doe',
      email: 'jane@example.com',
      avatarUrl: '',
    };
  });

  onToggleSidebar() {
    // TODO: ouvrir ton sidenav dashboard
  }

  onSignOut() {
    // TODO: déconnexion
  }
}
