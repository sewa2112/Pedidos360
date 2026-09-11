import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AccountInfo } from '@azure/msal-browser';
import { MsalService } from '@azure/msal-angular';
import { loginRequest, redirectUri } from '../auth-config';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="pagina">
      <section class="tarjeta">
        <div class="marca">
          <span class="marca-icono">P360</span>
          <div>
            <p class="subtitulo">Desarrollo Cloud Native I</p>
            <h1>{{ title }}</h1>
          </div>
        </div>

        <p class="descripcion">
          Aplicación Angular protegida mediante Microsoft Entra ID y MSAL.
        </p>

        @if (cargando) {
          <div class="estado">
            Inicializando autenticación...
          </div>
        } @else if (usuario) {
          <div class="usuario">
            <span class="indicador"></span>
            <div>
              <p class="etiqueta">Sesión iniciada</p>
              <h2>{{ usuario.name || 'Usuario autenticado' }}</h2>
              <p>{{ usuario.username }}</p>
            </div>
          </div>

          <button type="button" class="boton boton-principal" (click)="irAlDashboard()">
            Ir al dashboard
          </button>
          <button type="button" class="boton boton-secundario" (click)="cerrarSesion()">
            Cerrar sesión
          </button>
        } @else {
          <div class="estado">
            No existe una sesión activa.
          </div>
          <button type="button" class="boton boton-principal" (click)="iniciarSesion()">
            Iniciar sesión con Microsoft
          </button>
        }

        @if (mensajeError) {
          <p class="error">{{ mensajeError }}</p>
        }

        <footer>
          Frontend Angular · Microsoft Entra ID · MSAL
        </footer>
      </section>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      color: #172033;
    }

    * {
      box-sizing: border-box;
    }

    .pagina {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background:
        radial-gradient(circle at top left, rgba(49, 130, 246, 0.24), transparent 38%),
        linear-gradient(135deg, #eef5ff 0%, #f8fafc 55%, #e8f1ff 100%);
    }

    .tarjeta {
      width: min(100%, 560px);
      padding: 40px;
      background: rgba(255, 255, 255, 0.94);
      border: 1px solid rgba(148, 163, 184, 0.28);
      border-radius: 24px;
      box-shadow: 0 24px 70px rgba(15, 23, 42, 0.14);
    }

    .marca {
      display: flex;
      align-items: center;
      gap: 18px;
    }

    .marca-icono {
      width: 64px;
      height: 64px;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      color: #ffffff;
      font-size: 17px;
      font-weight: 800;
      background: linear-gradient(135deg, #2563eb, #0f4cbd);
      border-radius: 18px;
      box-shadow: 0 12px 25px rgba(37, 99, 235, 0.28);
    }

    .subtitulo {
      margin: 0 0 4px;
      color: #64748b;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 0.03em;
      text-transform: uppercase;
    }

    h1 {
      margin: 0;
      color: #0f172a;
      font-size: clamp(32px, 7vw, 46px);
      line-height: 1;
    }

    .descripcion {
      margin: 28px 0;
      color: #475569;
      font-size: 17px;
      line-height: 1.6;
    }

    .estado,
    .usuario {
      margin-bottom: 22px;
      padding: 18px;
      background: #f8fafc;
      border: 1px solid #dbe4ef;
      border-radius: 14px;
    }

    .usuario {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .usuario h2 {
      margin: 3px 0;
      color: #0f172a;
      font-size: 19px;
    }

    .usuario p {
      margin: 0;
      color: #64748b;
      word-break: break-word;
    }

    .usuario .etiqueta {
      color: #15803d;
      font-size: 13px;
      font-weight: 700;
      text-transform: uppercase;
    }

    .indicador {
      width: 12px;
      height: 12px;
      flex-shrink: 0;
      background: #22c55e;
      border-radius: 50%;
      box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.15);
    }

    .boton {
      width: 100%;
      padding: 14px 20px;
      border: 0;
      border-radius: 12px;
      font: inherit;
      font-weight: 700;
      cursor: pointer;
      transition: transform 150ms ease, box-shadow 150ms ease;
      margin-top: 10px;
    }

    .boton:hover {
      transform: translateY(-1px);
    }

    .boton-principal {
      color: #ffffff;
      background: #2563eb;
      box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
    }

    .boton-secundario {
      color: #1e3a8a;
      background: #dbeafe;
    }

    .error {
      margin: 18px 0 0;
      padding: 13px 15px;
      color: #991b1b;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 10px;
    }

    footer {
      margin-top: 28px;
      color: #94a3b8;
      font-size: 13px;
      text-align: center;
    }

    @media (max-width: 600px) {
      .tarjeta {
        padding: 28px 22px;
      }
    }
  `],
})
export class AuthPageComponent implements OnInit {
  title = 'Pedidos360';
  usuario: AccountInfo | null = null;
  cargando = true;
  mensajeError = '';

  private readonly authService = inject(MsalService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    try {
      await this.authService.instance.initialize();

      const resultado = await this.authService.instance.handleRedirectPromise();
      if (resultado?.account) {
        this.authService.instance.setActiveAccount(resultado.account);
      }

      this.actualizarUsuario();

      if (this.usuario) {
        await this.router.navigate(['/dashboard']);
      }
    } catch (error) {
      console.error('Error al inicializar MSAL:', error);
      this.mensajeError = 'No fue posible inicializar la autenticación.';
    } finally {
      this.cargando = false;
      this.cdr.detectChanges();
    }
  }

  iniciarSesion(): void {
    this.mensajeError = '';
    this.authService.loginRedirect(loginRequest).subscribe({
      error: (error) => {
        console.error('Error al iniciar sesión:', error);
        this.mensajeError = 'No fue posible iniciar sesión con Microsoft.';
        this.cdr.detectChanges();
      },
    });
  }

  irAlDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  cerrarSesion(): void {
    this.authService.logoutRedirect({
      account: this.usuario ?? undefined,
      postLogoutRedirectUri: redirectUri,
    }).subscribe({
      error: (error) => {
        console.error('Error al cerrar sesión:', error);
        this.mensajeError = 'No fue posible cerrar la sesión.';
        this.cdr.detectChanges();
      },
    });
  }

  private actualizarUsuario(): void {
    const cuentaActiva = this.authService.instance.getActiveAccount();
    const cuentas = this.authService.instance.getAllAccounts();
    this.usuario = cuentaActiva ?? cuentas[0] ?? null;

    if (this.usuario && !cuentaActiva) {
      this.authService.instance.setActiveAccount(this.usuario);
    }
  }
}
