import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'tchl-generic-info',
  imports: [CommonModule],
  template: `
    <section class="generic">
      <div class="title"><strong>Widget non reconnu</strong></div>
      <div class="hint">
        Composant: <code>{{ name() }}</code>
      </div>
    </section>
  `,
  styles: [
    `
      .generic {
        border: 2px dashed #e57373;
        background: #fff0f0;
        border-radius: 12px;
        padding: 12px;
      }
      .title {
        margin-bottom: 4px;
      }
      .hint {
        opacity: 0.85;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenericInfoWidgetComponent {
  name = input<string>('Unknown');
}
