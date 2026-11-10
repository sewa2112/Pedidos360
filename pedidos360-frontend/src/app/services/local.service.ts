import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Local {
  id?: number;
  nombre: string;
  direccion: string;
  telefono?: string;
  categoria?: string;
  activo?: boolean;
}

export interface PedidoCocina {
  id?: number;
  pedidoId: number;
  cliente: string;
  restaurante: string;
  items: string;
  direccion?: string;
  total?: number;
  estado: string;
  fechaCreacion?: string;
}

@Injectable({
  providedIn: 'root',
})
export class LocalService {
  private readonly http = inject(HttpClient);
  // En producción usa la ruta relativa /api/locales resuelta por NGINX hacia local-service:8082
  private readonly apiUrl = (environment as any).localApiUrl || '/api/locales';

  getLocales(): Observable<Local[]> {
    return this.http.get<Local[]>(this.apiUrl);
  }

  crearLocal(local: Local): Observable<Local> {
    return this.http.post<Local>(this.apiUrl, local);
  }

  obtenerLocal(id: number): Observable<Local> {
    return this.http.get<Local>(`${this.apiUrl}/${id}`);
  }

  getPedidosCocina(): Observable<PedidoCocina[]> {
    return this.http.get<PedidoCocina[]>(`${this.apiUrl}/pedidos`);
  }

  getPedidosPorRestaurante(restaurante: string): Observable<PedidoCocina[]> {
    return this.http.get<PedidoCocina[]>(`${this.apiUrl}/${encodeURIComponent(restaurante)}/pedidos`);
  }

  actualizarEstadoPedido(id: number, estado: string): Observable<PedidoCocina> {
    return this.http.put<PedidoCocina>(`${this.apiUrl}/pedidos/${id}/estado`, { estado });
  }
}
