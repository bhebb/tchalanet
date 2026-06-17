import { Route } from '@angular/router';

export const publicRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./home/public-home.page').then(m => m.PublicHomePage),
  },
  {
    path: 'check-ticket',
    loadComponent: () =>
      import('./check-ticket/public-check-ticket.page').then(m => m.PublicCheckTicketPage),
  },
  {
    path: 'results',
    pathMatch: 'full',
    loadComponent: () =>
      import('./results/public-results.page').then(m => m.PublicResultsPage),
  },
  {
    path: 'results/:drawResultId',
    loadComponent: () =>
      import('./results-detail/public-result-detail.page').then(m => m.PublicResultDetailPage),
  },
  {
    path: 'rules',
    loadComponent: () => import('./rules/public-rules.page').then(m => m.PublicRulesPage),
  },
  {
    path: 'tchala',
    loadComponent: () => import('./tchala/public-tchala.page').then(m => m.PublicTchalaPage),
  },
  {
    path: 'news',
    loadComponent: () => import('./news/public-news.page').then(m => m.PublicNewsPage),
  },
  {
    path: 'help',
    loadComponent: () => import('./help/public-help.page').then(m => m.PublicHelpPage),
  },
  {
    path: 'managers',
    loadComponent: () =>
      import('./managers/public-managers.page').then(m => m.PublicManagersPage),
  },
  {
    path: 'operators',
    loadComponent: () =>
      import('./operators/public-operators.page').then(m => m.PublicOperatorsPage),
  },
  {
    path: 'contact',
    loadComponent: () =>
      import('./contact/public-contact.page').then(m => m.PublicContactPage),
  },
  {
    path: 'privacy',
    loadComponent: () =>
      import('./markdown/public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { file: 'privacy' },
  },
  {
    path: 'terms',
    loadComponent: () =>
      import('./markdown/public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { file: 'terms' },
  },
];
