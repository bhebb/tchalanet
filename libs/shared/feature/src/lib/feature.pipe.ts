import { inject, Pipe, PipeTransform } from '@angular/core';
import { FeatureService } from './feature.service';

@Pipe({ name: 'featureEnabled', standalone: true, pure: false })
export class FeatureEnabledPipe implements PipeTransform {
  private features = inject(FeatureService);

  transform(flag?: string, def = true) {
    return this.features.isEnabled(flag, def);
  }
}
