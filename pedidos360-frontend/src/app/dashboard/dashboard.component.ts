import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountInfo } from '@azure/msal-browser';
import { MsalService } from '@azure/msal-angular';
import { redirectUri } from '../auth-config';
import { Pedido, PedidoService } from '../services/pedido.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  usuario: AccountInfo | null = null;
  pedidos: Pedido[] = [];
  cargando = true;
  error = '';

  nuevoPedido = {
    cliente: '',
    restaurante: '',
    items: '',
    direccion: '',
    montoTotal: 0,
    estado: 'PENDIENTE',
    fecha: new Date().toISOString().slice(0, 10),
  };

  private readonly authService = inject(MsalService);
  private readonly pedidoService = inject(PedidoService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    try {
      await this.authService.instance.initialize();
      this.usuario = this.authService.instance.getActiveAccount() ?? this.authService.instance.getAllAccounts()[0] ?? null;

      if (!this.usuario) {
        this.router.navigate(['/']);
        return;
      }

      this.cargarPedidos();
    } catch (error) {
      console.error('No se pudo inicializar MSAL en el dashboard:', error);
      this.router.navigate(['/']);
    }
  }

  cargarPedidos(): void {
    this.cargando = true;
    this.error = '';

    this.pedidoService.getPedidos().subscribe({
      next: (pedidos) => {
        this.pedidos = pedidos.map((pedido) => this.normalizarPedido(pedido));
        this.cargando = false;
      },
      error: (error) => {
        console.error('Error al cargar pedidos:', error);
        this.error = 'No se pudieron cargar los pedidos desde el backend.';
        this.cargando = false;
      },
    });
  }

  crearPedido(): void {
    if (!this.nuevoPedido.cliente || !this.nuevoPedido.restaurante || !this.nuevoPedido.items) {
      this.error = 'Completa cliente, restaurante y productos antes de guardar.';
      return;
    }

    const payload: Pedido = {
      cliente: this.nuevoPedido.cliente,
      restaurante: this.nuevoPedido.restaurante,
      items: this.nuevoPedido.items,
      direccion: this.nuevoPedido.direccion,
      montoTotal: Number(this.nuevoPedido.montoTotal) || 0,
      estado: this.nuevoPedido.estado,
      fecha: this.nuevoPedido.fecha,
    };

    this.pedidoService.crearPedido(payload).subscribe({
      next: (pedidoCreado) => {
        this.pedidos.unshift(this.normalizarPedido(pedidoCreado));
        this.nuevoPedido = {
          cliente: '',
          restaurante: '',
          items: '',
          direccion: '',
          montoTotal: 0,
          estado: 'PENDIENTE',
          fecha: new Date().toISOString().slice(0, 10),
        };
        this.error = '';
      },
      error: (error) => {
        console.error('Error al crear pedido:', error);
        this.error = 'No se pudo guardar el pedido.';
      },
    });
  }

  actualizarEstadoPedido(id: number | undefined, estado: string): void {
    if (!id) {
      return;
    }

    const pedido = this.pedidos.find((item) => item.id === id);
    if (!pedido) {
      return;
    }

    const payload: Pedido = {
      ...pedido,
      estado,
    };

    this.pedidoService.actualizarPedido(id, payload).subscribe({
      next: (pedidoActualizado) => {
        const index = this.pedidos.findIndex((item) => item.id === id);
        if (index >= 0) {
          this.pedidos[index] = this.normalizarPedido(pedidoActualizado);
        }
      },
      error: (error) => {
        console.error('Error al actualizar pedido:', error);
        this.error = 'No se pudo actualizar el estado del pedido.';
      },
    });
  }

  eliminarPedido(id: number | undefined): void {
    if (!id) {
      return;
    }

    this.pedidoService.eliminarPedido(id).subscribe({
      next: () => {
        this.pedidos = this.pedidos.filter((pedido) => pedido.id !== id);
      },
      error: (error) => {
        console.error('Error al eliminar pedido:', error);
        this.error = 'No se pudo eliminar el pedido.';
      },
    });
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

  formatearMonto(monto: number | string | null | undefined): string {
    const valor = Number(monto ?? 0);
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(valor);
  }

  estadoClase(estado: string = ''): string {
    const estadoUpper = estado.toUpperCase();
    switch (estadoUpper) {
      case 'PENDIENTE':
        return 'bg-amber-100 text-amber-800 border border-amber-300';
      case 'EN CAMINO':
        return 'bg-purple-100 text-purple-800 border border-purple-300';
      case 'ENTREGADO':
        return 'bg-emerald-100 text-emerald-800 border border-emerald-300';
      default:
        return 'bg-slate-100 text-slate-700 border border-slate-300';
    }
  }

  private normalizarPedido(pedido: Pedido): Pedido {
    return {
      ...pedido,
      cliente: pedido.cliente ?? pedido.nombre_cliente ?? 'Sin cliente',
      items: pedido.items ?? pedido.productos ?? 'Sin productos',
      montoTotal: pedido.montoTotal ?? pedido.monto_Total ?? 0,
      estado: pedido.estado ?? 'PENDIENTE',
    };
  }
}
