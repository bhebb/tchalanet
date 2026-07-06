import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-console-draw-result-raw',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './console-draw-result-raw.component.html',
  styleUrls: ['./console-draw-result-raw.component.scss'],
})
export class ConsoleDrawResultRawComponent {
  readonly content = input.required<string | null>();
  readonly emptyLabel = input('Aucun résultat brut disponible.');
}
