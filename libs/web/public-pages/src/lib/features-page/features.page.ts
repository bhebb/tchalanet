import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';

import { FeatureDirective, FeatureEnabledPipe, FeatureService } from '@tchl/feature';

@Component({
  selector: 'tchl-features-page',
  standalone: true,
  imports: [CommonModule, FeatureEnabledPipe, FeatureDirective],
  template: `<h1>Feature diagnostics</h1>
    <section>
      <h3>Directive</h3>
      <p *tchFeature="'test'; else off">Flag "test" is ON</p>
      <ng-template #off>OFF</ng-template>
    </section>

    <section>
      <h3>Pipe</h3>
      test → {{ 'test' | featureEnabled }}
    </section>

    <section>
      <h3>Service</h3>
      <div>isEnabled('test'): {{ enabled() }}</div>
    </section> `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [],
})
export class FeaturesPage {
  private features = inject(FeatureService);
  enabled = signal(false);
  snapshot = signal<any>({});

  constructor() {
    effect(() => this.enabled.set(this.features.isEnabled('test')));
    this.enabled.set(this.features.isEnabled('test'));
  }
}
