import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

export interface GamesSetupIssue {
  readonly gameCode: string;
  readonly gameName: string;
  readonly messageKey: string;
  readonly actionLabelKey: string;
  readonly action: 'configure-game';
  readonly tone: 'warning' | 'danger';
}

@Component({
  selector: 'tch-games-setup-issues',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, TranslatePipe],
  templateUrl: './games-setup-issues.component.html',
  styleUrls: ['./games-setup-issues.component.scss'],
})
export class GamesSetupIssuesComponent {
  readonly issues = input.required<readonly GamesSetupIssue[]>();
  readonly issueAction = output<GamesSetupIssue>();
}
