import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'tch-access-state-page',
  template: `
    <section class="page">
      <h1>{{ title }}</h1>
      <p>{{ body }}</p>
      <a routerLink="/app">Retour</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
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
export class AccessStatePage {
  private readonly route = inject(ActivatedRoute);

  readonly title = this.route.snapshot.data['title'] ?? 'Accès indisponible';
  readonly body =
    this.route.snapshot.data['body'] ??
    "Votre accès n'est pas encore configuré pour cette application.";
}
