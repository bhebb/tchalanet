import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'tch-forbidden-page',
  template: `
    <section class="page">
      <h1>Forbidden</h1>
      <p>Your current role does not allow access to this surface.</p>
      <a routerLink="/public">Back to public home</a>
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
