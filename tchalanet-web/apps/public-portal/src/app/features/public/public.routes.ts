import { Route } from '@angular/router';

const publicFeatureRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./home/public-home.page').then(m => m.PublicHomePage),
    data: { titleKey: 'public.nav.home' },
  },
  {
    path: 'check-ticket',
    loadComponent: () =>
      import('./check-ticket/public-check-ticket.page').then(m => m.PublicCheckTicketPage),
    data: { titleKey: 'public.check.title' },
  },
  {
    path: 'verify',
    loadComponent: () =>
      import('./check-ticket/public-check-ticket.page').then(m => m.PublicCheckTicketPage),
    data: { titleKey: 'public.check.title' },
  },
  {
    path: 'ticket/:publicCode',
    loadComponent: () =>
      import('./check-ticket/public-check-ticket.page').then(m => m.PublicCheckTicketPage),
    data: { titleKey: 'public.check.title' },
  },
  {
    path: 'results',
    pathMatch: 'full',
    loadComponent: () =>
      import('./results/public-results.page').then(m => m.PublicResultsPage),
    data: { titleKey: 'public.nav.results' },
  },
  {
    path: 'results/:drawResultId',
    loadComponent: () =>
      import('./results-detail/public-result-detail.page').then(m => m.PublicResultDetailPage),
    data: { titleKey: 'public.results.receipt_title' },
  },
  {
    path: 'rules',
    loadComponent: () => import('./rules/public-rules.page').then(m => m.PublicRulesPage),
    data: { titleKey: 'public.nav.rules' },
  },
  {
    path: 'tchala',
    loadComponent: () => import('./tchala/public-tchala.page').then(m => m.PublicTchalaPage),
    data: { titleKey: 'public.tchala.title' },
  },
  {
    path: 'news',
    loadComponent: () => import('./news/public-news.page').then(m => m.PublicNewsPage),
    data: { titleKey: 'public.news.title' },
  },
  {
    path: 'help',
    loadComponent: () => import('./help/public-help.page').then(m => m.PublicHelpPage),
    data: { titleKey: 'public.nav.help' },
  },
  {
    path: 'managers',
    loadComponent: () =>
      import('./managers/public-managers.page').then(m => m.PublicManagersPage),
    data: { titleKey: 'public.nav.managers' },
  },
  {
    path: 'operators',
    loadComponent: () =>
      import('./operators/public-operators.page').then(m => m.PublicOperatorsPage),
    data: { titleKey: 'public.nav.operators' },
  },
  {
    path: 'contact',
    loadComponent: () =>
      import('./contact/public-contact.page').then(m => m.PublicContactPage),
    data: { titleKey: 'public.footer.support.contact' },
  },
  {
    path: 'support',
    loadComponent: () =>
      import('./support/public-support.page').then(m => m.PublicSupportPage),
    data: { titleKey: 'public.footer.support.title' },
  },
  {
    path: 'privacy',
    loadComponent: () =>
      import('./markdown/public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { titleKey: 'public.footer.legal.privacy', file: 'privacy' },
  },
  {
    path: 'terms',
    loadComponent: () =>
      import('./markdown/public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { titleKey: 'public.footer.legal.terms', file: 'terms' },
  },
  {
    path: 'data-deletion',
    loadComponent: () =>
      import('./data-deletion/public-data-deletion.page').then(m => m.PublicDataDeletionPage),
    data: { titleKey: 'public.footer.legal.data_deletion' },
  },
];

export const publicRoutes: Route[] = [
  ...publicFeatureRoutes,
  {
    path: 'public',
    children: publicFeatureRoutes,
  },
];
