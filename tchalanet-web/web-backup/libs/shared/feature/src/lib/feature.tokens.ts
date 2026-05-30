import { InjectionToken } from '@angular/core';

import { FeatureClient, FeatureContext, FeatureKey } from './feature.types';

export const FEATURE_CLIENT = new InjectionToken<FeatureClient>('FEATURE_CLIENT');
export const FEATURE_INITIAL = new InjectionToken<FeatureKey[]>('FEATURE_INITIAL'); // PageModel.features
export const FEATURE_CONTEXT = new InjectionToken<FeatureContext>('FEATURE_CONTEXT');
