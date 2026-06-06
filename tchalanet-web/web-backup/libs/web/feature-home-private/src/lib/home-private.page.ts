import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'lib-home-private-page',
  standalone: true,
  imports: [RouterLink],
  template: `
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
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePrivatePage {}
