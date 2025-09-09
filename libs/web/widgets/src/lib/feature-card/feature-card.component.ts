import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FeatureCardWidgetProps } from '@tchl/types';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tchl-feature-card-widget',
  standalone: true,
  imports: [CommonModule, MatIconModule, TranslatePipe],
  template: `
    <article class="feature-card"
             [class.feature-card--with-image]="!!props().image">
      @if (props().image) {
        <div class="feature-card__image">
          <img [src]="props().image!.src" 
               [alt]="props().image!.alt || ''"
               loading="lazy" 
               decoding="async" />
        </div>
      }

      <!-- Ligne icône + titre -->
      <div class="feature-card__head">
        @if (props().icon) {
          <span class="feature-card__icon material-symbols-outlined">{{ props().icon }}</span>
        }
        <h3 class="feature-card__title">
          {{ props().title | translate }}
        </h3>
      </div>

      @if (props().description) {
        <p class="feature-card__desc">
          {{ props().description | translate }}
        </p>
      }

      @if (props().link) {
        <a class="feature-card__link" [href]="props().link!.path">
          {{ props().link!.labelKey | translate }}
        </a>
      }
    </article>
  `,
  styleUrl: './feature-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeatureCardWidgetComponent {
  props = input.required<FeatureCardWidgetProps>();
}
