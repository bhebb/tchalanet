import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tch-setup-progress-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, MatProgressBarModule],
  templateUrl: './setup-progress-header.component.html',
  styleUrls: ['./setup-progress-header.component.scss'],
})
export class SetupProgressHeaderComponent {
  readonly completed = input.required<number>();
  readonly total = input.required<number>();
  readonly pct = input.required<number>();
  readonly complete = input(false);
}
