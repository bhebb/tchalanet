import { Routes } from '@angular/router';
import { MarkdownPageComponent, PublicStubPageComponent } from '@tchl/web/shell';
import { PlansPage } from './plans-page/plans.page';
import { FeaturesPage } from './features-page/features.page';

export const PUBLIC_ROUTES: Routes = [
  { path: 'pricing', component: PlansPage, data: { breadcrumb: 'nav.pricing' } },
  { path: 'features', component: FeaturesPage, data: { breadcrumb: 'nav.features' } },
  {
    path: 'verify',
    component: PublicStubPageComponent,
    data: { titleKey: 'nav.verify', descKey: 'seo.verify', breadcrumb: 'nav.verify' },
  },
  {
    path: 'ticket/:code',
    component: PublicStubPageComponent,
    data: {
      titleKey: 'seo.ticket.title',
      descKey: 'seo.ticket.desc',
      breadcrumb: 'nav.ticket',
      robotsNoIndex: true,
    },
  },
  {
    path: 'explanations',
    component: MarkdownPageComponent,
    data: { slug: 'explanations', breadcrumb: 'nav.explanations' },
  },
  {
    path: 'official-reports',
    component: MarkdownPageComponent,
    data: { slug: 'official-reports', breadcrumb: 'nav.official_reports' },
  },
  {
    path: 'security',
    component: MarkdownPageComponent,
    data: { slug: 'security', breadcrumb: 'nav.security' },
  },
  {
    path: 'tchala',
    component: MarkdownPageComponent,
    data: { slug: 'tchala', breadcrumb: 'nav.tchala' },
  },
  {
    path: 'legal/regulation',
    component: MarkdownPageComponent,
    data: { slug: 'legal/regulation', breadcrumb: 'nav.legal.regulation' },
  },
  {
    path: 'legal/responsible',
    component: MarkdownPageComponent,
    data: { slug: 'legal/responsible', breadcrumb: 'nav.legal.responsible' },
  },
  {
    path: 'legal/privacy',
    component: MarkdownPageComponent,
    data: { slug: 'legal/privacy', breadcrumb: 'crumb.legal privacy' },
  },
  {
    path: 'support',
    component: MarkdownPageComponent,
    data: { slug: 'support', breadcrumb: 'nav.support' },
  },
];
