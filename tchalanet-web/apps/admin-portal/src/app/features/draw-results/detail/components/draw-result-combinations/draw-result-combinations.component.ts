import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { DrawCombinationGameSection } from '@tch/web/console';

/** Tab « Combinaisons & règles » — présentation seule des numéros gagnants par option supportée. */
@Component({
  selector: 'tch-draw-result-combinations',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminEmptyStateComponent],
  templateUrl: './draw-result-combinations.component.html',
  styleUrls: ['./draw-result-combinations.component.scss'],
})
export class DrawResultCombinationsComponent {
  readonly rows = input.required<readonly DrawCombinationGameSection[]>();
}
