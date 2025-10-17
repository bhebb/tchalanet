import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '@tchl/shared/auth';

@Component({
  standalone: true,
  template: `
    <div style="min-height:60vh;display:grid;place-items:center;gap:12px">
      <mat-progress-spinner mode="indeterminate"></mat-progress-spinner>
      <p>Redirection vers la page de connexion…</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatProgressSpinnerModule],
})
export class AuthLoginComponent implements OnInit {
  private auth: AuthService = inject(AuthService);

  ngOnInit() {
    sessionStorage.setItem('login_target', '/app/dashboard'); // ou '/app/dashboard'
    this.auth.login('/app');
  }
}
