import { Routes } from '@angular/router';
import { autoLoginPartialRoutesGuard } from 'angular-auth-oidc-client';
import { Dashboard } from './dashboard/dashboard';

export const routes: Routes = [
  { path: '', component: Dashboard, canActivate: [autoLoginPartialRoutesGuard] },
  { path: '**', redirectTo: '' },
];
