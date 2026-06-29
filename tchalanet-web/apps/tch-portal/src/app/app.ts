import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  imports: [RouterModule],
  selector: 'tch-root',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app.scss',
})
export class App {
  protected title = 'tch-portal';

  // Runtime bootstrap is owned by each shell: TchPublicShellComponent inits the
  // public runtime, PrivateShellPage the private one. Landing directly on /app/**
  // must not trigger any public API call.
}
