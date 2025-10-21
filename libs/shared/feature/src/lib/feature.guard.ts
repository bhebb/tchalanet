import { CanMatchFn } from '@angular/router';
import { inject } from '@angular/core';
import { FeatureService } from './feature.service';

export const featureGuard: CanMatchFn = route => {
  const flag = route.data?.['feature'] as string | undefined;
  return inject(FeatureService).isEnabled(flag);
};
