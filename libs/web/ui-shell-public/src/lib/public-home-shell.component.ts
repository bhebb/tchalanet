// libs/web/ui-shell-public/src/lib/public-home-shell.component.ts
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PublicHomeShellStore } from './public-home-shell.store';
import { FooterComponent, HeaderComponent } from 'shared/ui-shell-material';

@Component({
  selector: 'lib-public-home-shell',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  providers: [PublicHomeShellStore], // scope par route
  template: `
    <lib-header
      [model]="store.header()"
      (langChange)="store.onLangChange($event)">
    </lib-header>

    <main class="container" style="min-height:60vh; padding-block:24px">
      <router-outlet />
    </main>

    <lib-footer [model]="store.footer()"></lib-footer>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublicHomeShellComponent {
  readonly store = inject(PublicHomeShellStore);
}
