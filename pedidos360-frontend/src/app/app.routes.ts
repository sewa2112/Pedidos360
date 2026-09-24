import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { AuthPageComponent } from './auth-page/auth-page.component';
import { DashboardComponent } from './dashboard/dashboard.component';

export const routes: Routes = [
  { path: '', component: AuthPageComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [MsalGuard] },
  { path: '**', redirectTo: '' }
];
