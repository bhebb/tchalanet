import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { BreadcrumbComponent } from '@tchl/breadcrumb';
import { PageFacade, SessionFacade } from '@tchl/facades';
import { AuthService } from '@tchl/shared/auth';
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
  private readonly session = inject(SessionFacade);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  page = this.pageFacade.page;

  flags = computed(() => this.page()?.flags ?? []);

  headerProps = computed(() => {
    const p = this.page();
    return p?.header?.properties;
  });

  sideLinks = computed(() => this.page()?.nav?.sidenav ?? []);

  userInfo = computed(() => {
    const displayName = this.session.displayName();
    const email = this.session.email();
    const tch = this.auth.tch();
    return {
      displayName: displayName || undefined,
      email: email,
      avatarUrl: '',
      roles: tch?.roles ?? [],
      tenantId: tch?.tenantId,
      plan: tch?.plan,
    };
  });

  onToggleSidebar() {
    // TODO: ouvrir ton sidenav dashboard
  }

  onSignOut() {
    this.auth.logout();
  }
}
