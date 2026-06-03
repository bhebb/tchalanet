import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageSwitcherComponent } from '../../core/i18n';

@Component({
  imports: [RouterLink, TranslatePipe, LanguageSwitcherComponent],
  selector: 'tch-forbidden-page',
  template: `
    <section class="page">
      <tch-language-switcher />
      <h1>{{ 'auth.forbidden.title' | translate }}</h1>
      <p>{{ 'auth.forbidden.body' | translate }}</p>
      <a routerLink="/public">{{ 'auth.forbidden.back' | translate }}</a>
    </section>
  `,
  styles: [
    `
      .page {
        display: grid;
        gap: 1rem;
        max-width: 720px;
        padding: 2rem;
      }
    `,
  ],
})
export class ForbiddenPage {}
