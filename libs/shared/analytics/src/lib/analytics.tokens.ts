import { InjectionToken } from '@angular/core';
import { AnalyticsConfig } from './analytics.types';

export const ANALYTICS_CONFIG = new InjectionToken<AnalyticsConfig>('ANALYTICS_CONFIG');
