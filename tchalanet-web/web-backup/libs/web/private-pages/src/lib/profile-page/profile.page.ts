import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { SessionFacade } from '@tchl/facades';
import { AuthService } from '@tchl/shared/auth';

@Component({
  selector: 'tchl-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  private readonly session = inject(SessionFacade);
  private readonly auth = inject(AuthService);

  readonly user = computed(() => ({
    displayName: this.session.displayName(),
    email: this.session.email(),
  }));

  readonly tch = this.auth.tch;
}
