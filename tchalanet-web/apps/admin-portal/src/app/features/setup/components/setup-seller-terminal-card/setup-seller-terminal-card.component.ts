import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import type { AdminSectionTargetError } from '@tch/ui/console';

@Component({
  selector: 'tch-setup-seller-terminal-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslatePipe, MatButtonModule, MatIconModule, TchSectionError],
  templateUrl: './setup-seller-terminal-card.component.html',
  styleUrls: ['./setup-seller-terminal-card.component.scss'],
})
export class SetupSellerTerminalCardComponent {
  readonly unlocked = input.required<boolean>();
  readonly blockingSteps = input<readonly string[]>([]);
  readonly sectionError = input<AdminSectionTargetError | null>(null);
}
