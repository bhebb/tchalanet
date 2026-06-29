import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'tch-access-state-page',
  templateUrl: './access-state.page.html',
  styleUrl: './access-state.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccessStatePage {
  private readonly route = inject(ActivatedRoute);

  readonly title = this.route.snapshot.data['title'] ?? 'Accès indisponible';
  readonly body =
    this.route.snapshot.data['body'] ??
    "Votre accès n'est pas encore configuré pour cette application.";
}
