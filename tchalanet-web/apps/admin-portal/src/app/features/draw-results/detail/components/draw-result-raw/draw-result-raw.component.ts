import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/** Tab « Résultat brut » — présentation seule du payload source JSON. */
@Component({
  selector: 'tch-draw-result-raw',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './draw-result-raw.component.html',
  styleUrls: ['./draw-result-raw.component.scss'],
})
export class DrawResultRawComponent {
  /** Pretty-printed JSON payload, or null when no raw data is available. */
  readonly content = input.required<string | null>();
}
