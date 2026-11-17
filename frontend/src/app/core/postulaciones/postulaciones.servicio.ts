import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { Observable } from 'rxjs';

import { ToastServicio } from '../../shared/toast/toast.servicio';
import { API_POSTULACIONES } from './postulaciones.api';
import { ClicPostular, HojaUsada, ListaPostulaciones, OrdenPostulaciones, Postulacion } from './postulaciones.modelos';
import { PestanaPendiente } from './pestana-externa';

export type EstadoLista = 'cargando' | 'listo' | 'error';

/**
 * Historial de clics en «Postular». Todo viene del API real; sin backend la pantalla muestra su error.
 * El backend solo devuelve y modifica los registros de quien inició sesión.
 */
@Injectable({ providedIn: 'root' })
export class PostulacionesServicio {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastServicio);
  private readonly router = inject(Router);
  private readonly i18n = inject(TranslocoService);

  readonly items = signal<readonly Postulacion[]>([]);
  readonly clics90 = signal<number | null>(null);
  readonly estado = signal<EstadoLista>('cargando');

  cargar(texto: string, orden: OrdenPostulaciones): void {
    this.estado.set('cargando');
    let params = new HttpParams().set('orden', orden);
    if (texto.trim()) params = params.set('q', texto.trim());
    this.http.get<ListaPostulaciones>(API_POSTULACIONES.lista, { params }).subscribe({
      next: (r) => {
        this.items.set(r.items);
        this.clics90.set(r.clicsUltimos90Dias);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  quitar(id: string): Observable<unknown> {
    return this.http.delete(API_POSTULACIONES.postulacion(id));
  }

  restaurar(id: string): Observable<unknown> {
    return this.http.post(API_POSTULACIONES.restaurar(id), {});
  }

  clicDesdeHistorial(id: string): Observable<ClicPostular> {
    return this.http.post<ClicPostular>(API_POSTULACIONES.clicPostulacion(id), {});
  }

  clicEnVacante(vacanteId: string, hoja: HojaUsada): Observable<ClicPostular> {
    return this.http.post<ClicPostular>(API_POSTULACIONES.clicVacante(vacanteId), { hoja });
  }

  /**
   * «Postular» desde una vacante: registra el clic y lleva la pestaña (abierta dentro del clic con
   * `abrirEnBlanco`) a la página original de la oferta, donde la persona termina de postularse.
   */
  postularEnVacante(vacanteId: string, hoja: HojaUsada, pestana: PestanaPendiente): void {
    this.clicEnVacante(vacanteId, hoja).subscribe({
      next: (c) => {
        if (!pestana.completar(c.urlOriginal)) {
          this.toast.mostrar(this.i18n.translate('postulaciones.errorAbrir'));
          return;
        }
        this.toast.mostrar(this.i18n.translate('vacantes.postular.guardado'), {
          etiqueta: this.i18n.translate('vacantes.postular.verHistorial'),
          alHacer: () => void this.router.navigate(['/postulaciones']),
        });
      },
      error: () => {
        pestana.cancelar();
        this.toast.mostrar(this.i18n.translate('vacantes.postular.error'));
      },
    });
  }
}
