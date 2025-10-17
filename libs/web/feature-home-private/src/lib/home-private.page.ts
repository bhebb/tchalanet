import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FooterComponent, HeaderComponent } from 'shared/ui-shell-material';
import { RouterLink } from '@angular/router';
import { FooterModel, HeaderModel } from 'shared/types';

@Component({
  selector: 'lib-home-private-page',
  standalone: true,
  imports: [HeaderComponent, FooterComponent, RouterLink],
  template: `
    <!-- profil admin (compact/dark) -->
    <!-- ex: document.documentElement.setAttribute('data-profile','admin') après login -->
    <lib-header [model]="header" (langChange)="onLang($event)"></lib-header>

    <div style="display:grid; grid-template-columns: 240px 1fr; min-height:calc(100dvh - 128px);">
      <!-- Sidebar simple (remplace par Matero layout si besoin) -->
      <aside style="border-right:1px solid var(--mat-sys-outline-variant); padding:12px">
        <nav>
          <a routerLink="/admin" class="mat-mdc-button mat-mdc-button-base">Dashboard</a>
          <a routerLink="/admin/transactions" class="mat-mdc-button mat-mdc-button-base">Transactions</a>
        </nav>
      </aside>

      <main style="padding:16px">
        <h1>Accueil connecté</h1>
        <p>… ton dashboard / widgets …</p>
      </main>
    </div>

    <lib-footer [model]="footer"></lib-footer>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePrivatePage {
  header: HeaderModel = {
    logoUrl: '/assets/brand/logo.svg',
    menu: [{ id: 'dash', label: 'Dashboard', route: '/admin' }],
    langs: ['fr', 'en'],
    currentLang: 'fr'
  };
  footer: FooterModel = { columns: [], note: '© 2025 Tchalanet' };

  onLang(lang: string) { /* i18n service */
  }
}
