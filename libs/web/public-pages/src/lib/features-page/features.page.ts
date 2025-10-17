import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tchl-features-page',
  standalone: true,
  imports: [CommonModule],
  template: ` feature pages `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [],
})
export class FeaturesPage {}
