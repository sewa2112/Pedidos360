import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AccountInfo } from '@azure/msal-browser';
import { MsalService } from '@azure/msal-angular';
import { redirectUri } from '../auth-config';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent {
  usuario: AccountInfo | null = null;

  private readonly authService = inject(MsalService);
  private readonly router = inject(Router);

  constructor() {
    this.usuario = this.authService.instance.getActiveAccount() ?? this.authService.instance.getAllAccounts()[0] ?? null;
  }

  cerrarSesion(): void {
    this.authService.logoutRedirect({
      account: this.usuario ?? undefined,
      postLogoutRedirectUri: redirectUri,
    }).subscribe({
      error: (error) => {
        console.error('Error al cerrar sesión:', error);
      },
    });
  }

  volverAlLogin(): void {
    this.router.navigate(['/']);
  }
}
