import { ChangeDetectionStrategy, Component, input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroWidgetProps } from '@tchl/types';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'tchl-hero-widget',
  standalone: true,
  imports: [CommonModule, MatIconModule, TranslatePipe, MatButtonModule],
  template: `
    <section class="hero">
      <div class="hero__inner">
        <div class="hero__content">
          @if (props().eyebrow) {
          <p class="eyebrow">{{ props().eyebrow | translate }}</p>
          }
          <h1 class="title">{{ props().title | translate }}</h1>
          @if (props().lead) {
          <p class="lead">{{ props().lead | translate }}</p>
          }
          <div class="cta">
            @if (props().primaryCta) {
            <button mat-flat-button color="primary" class="btn btn--primary btn--with-dot">
              {{ props().primaryCta!.labelKey | translate }}
            </button>
            } @if (props().secondaryCta) {
            <button mat-stroked-button class="btn btn--ghost">
              {{ props().secondaryCta!.labelKey | translate }}
            </button>
            }
          </div>
        </div>

        @if (props().image; as img) {
        <div class="hero__art">
          <img [src]="img.src || props().imageUrl" [alt]="img.alt || ''" width="560" height="420" />
        </div>
        }
      </div>
    </section>
  `,
  styleUrl: './hero.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroWidgetComponent implements OnInit {
  props = input.required<HeroWidgetProps>();

  ngOnInit(): void {
    console.log('signal', this.props());
  }
}
