import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AccountInfo } from '@azure/msal-browser';
import { MsalService } from '@azure/msal-angular';
import { redirectUri } from '../auth-config';
import { Local, PedidoCocina, LocalService } from '../services/local.service';

@Component({
  selector: 'app-locales',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './locales.component.html',
  styleUrl: './locales.css',
})
export class LocalesComponent implements OnInit {
  usuario: AccountInfo | null = null;
  locales: Local[] = [];
  pedidosCocina: PedidoCocina[] = [];
  cargandoLocales = true;
  cargandoPedidos = true;
  error = '';
  mensajeExito = '';

  tabActiva: 'locales' | 'cocina' = 'locales';
  filtroRestaurante = 'TODOS';

  nuevoLocal: Local = {
    nombre: '',
    direccion: '',
    telefono: '',
    categoria: 'Pizzería',
    activo: true,
  };

  categoriasDisponibles = [
    'Pizzería',
    'Hamburguesas',
    'Sushi',
    'Comida Rápida',
    'Cafetería',
    'Mexicana',
    'Italiana',
    'Postres & Helados'
  ];

  private readonly authService = inject(MsalService);
  private readonly localService = inject(LocalService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    try {
      await this.authService.instance.initialize();
      this.usuario = this.authService.instance.getActiveAccount() ?? this.authService.instance.getAllAccounts()[0] ?? null;

      if (!this.usuario) {
        this.router.navigate(['/']);
        return;
      }

      this.cargarLocales();
      this.cargarPedidosCocina();
    } catch (err) {
      console.error('Error al inicializar sesión en locales:', err);
      this.router.navigate(['/']);
    }
  }

  cargarLocales(): void {
    this.cargandoLocales = true;
    this.error = '';

    this.localService.getLocales().subscribe({
      next: (locales) => {
        this.locales = locales;
        this.cargandoLocales = false;
      },
      error: (err) => {
        console.error('Error al cargar locales:', err);
        this.error = 'No se pudieron cargar los locales desde local-service.';
        this.cargandoLocales = false;
      },
    });
  }

  cargarPedidosCocina(): void {
    this.cargandoPedidos = true;
    this.localService.getPedidosCocina().subscribe({
      next: (pedidos) => {
        this.pedidosCocina = pedidos;
        this.cargandoPedidos = false;
      },
      error: (err) => {
        console.error('Error al cargar pedidos de cocina:', err);
        this.cargandoPedidos = false;
      },
    });
  }

  crearLocal(): void {
    if (!this.nuevoLocal.nombre.trim() || !this.nuevoLocal.direccion.trim()) {
      this.error = 'Por favor ingresa nombre y dirección del local.';
      return;
    }

    this.localService.crearLocal(this.nuevoLocal).subscribe({
      next: (creado) => {
        this.locales.push(creado);
        this.nuevoLocal = {
          nombre: '',
          direccion: '',
          telefono: '',
          categoria: 'Pizzería',
          activo: true,
        };
        this.error = '';
        this.mensajeExito = `Local "${creado.nombre}" registrado exitosamente.`;
        setTimeout(() => (this.mensajeExito = ''), 4000);
      },
      error: (err) => {
        console.error('Error al registrar local:', err);
        this.error = 'No se pudo guardar el local. Revisa la conexión con local-service.';
      },
    });
  }

  actualizarEstadoCocina(pedidoId: number | undefined, nuevoEstado: string): void {
    if (!pedidoId) return;

    this.localService.actualizarEstadoPedido(pedidoId, nuevoEstado).subscribe({
      next: (actualizado) => {
        const idx = this.pedidosCocina.findIndex((p) => p.id === pedidoId);
        if (idx !== -1) {
          this.pedidosCocina[idx].estado = nuevoEstado;
        }
        this.mensajeExito = `Comanda #${actualizado.id} actualizada a: ${nuevoEstado}`;
        setTimeout(() => (this.mensajeExito = ''), 3000);
      },
      error: (err) => {
        console.error('Error al actualizar estado:', err);
        this.error = 'No se pudo actualizar el estado de la comanda en cocina.';
      },
    });
  }

  get pedidosFiltrados(): PedidoCocina[] {
    if (this.filtroRestaurante === 'TODOS') {
      return this.pedidosCocina;
    }
    return this.pedidosCocina.filter(
      (p) => p.restaurante?.toLowerCase() === this.filtroRestaurante.toLowerCase()
    );
  }

  estadoClase(estado?: string): string {
    switch (estado?.toUpperCase()) {
      case 'ENTREGADO':
      case 'LISTO':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'EN PREPARACION':
      case 'EN_PREPARACION':
      case 'EN CAMINO':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'RECIBIDO':
      case 'PENDIENTE':
      default:
        return 'bg-blue-100 text-blue-800 border-blue-300';
    }
  }

  cerrarSesion(): void {
    this.authService.logoutRedirect({
      postLogoutRedirectUri: redirectUri,
    });
  }
}
