import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./components/dashboard/dashboard.component')
        .then(m => m.DashboardComponent)
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./components/history/history.component')
        .then(m => m.HistoryComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
