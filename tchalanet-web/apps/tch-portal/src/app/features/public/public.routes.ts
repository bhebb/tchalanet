import { Route } from '@angular/router';

export const publicRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./public-home.page').then(m => m.PublicHomePage),
  },
  {
    path: 'check-ticket',
    loadComponent: () => import('./public-check-ticket.page').then(m => m.PublicCheckTicketPage),
  },
  {
    path: 'results',
    pathMatch: 'full',
    loadComponent: () => import('./public-results.page').then(m => m.PublicResultsPage),
  },
  {
    path: 'results/:drawResultId',
    loadComponent: () => import('./public-result-detail.page').then(m => m.PublicResultDetailPage),
  },
  {
    path: 'rules',
    loadComponent: () => import('./public-rules.page').then(m => m.PublicRulesPage),
  },
  {
    path: 'tchala',
    loadComponent: () => import('./public-tchala.page').then(m => m.PublicTchalaPage),
  },
  {
    path: 'news',
    loadComponent: () => import('./public-news.page').then(m => m.PublicNewsPage),
  },
  {
    path: 'help',
    loadComponent: () => import('./public-help.page').then(m => m.PublicHelpPage),
  },
  {
    path: 'managers',
    loadComponent: () => import('./public-managers.page').then(m => m.PublicManagersPage),
  },
  {
    path: 'contact',
    loadComponent: () => import('./contact/public-contact.page').then(m => m.PublicContactPage),
  },
  {
    path: 'privacy',
    loadComponent: () => import('./public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { file: 'privacy' },
  },
  {
    path: 'terms',
    loadComponent: () => import('./public-markdown.page').then(m => m.PublicMarkdownPage),
    data: { file: 'terms' },
  },
];
