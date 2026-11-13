import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ADAPTACION } from './adaptacion.api';
import { Adaptacion, EstadoAdaptacion } from './adaptacion.modelos';

@Injectable({ providedIn: 'root' })
export class AdaptacionServicio {
  private readonly http = inject(HttpClient);

  estado(vacanteId: string): Observable<EstadoAdaptacion> {
    return this.http.get<EstadoAdaptacion>(API_ADAPTACION.deVacante(vacanteId));
  }

  /** Crea la adaptación (o la regenera). Consume un cupo del día. */
  crear(vacanteId: string): Observable<Adaptacion> {
    return this.http.post<Adaptacion>(API_ADAPTACION.deVacante(vacanteId), {});
  }

  descartar(id: string): Observable<unknown> {
    return this.http.delete(API_ADAPTACION.adaptacion(id));
  }

  aceptar(id: string, postular: boolean): Observable<unknown> {
    return this.http.post(API_ADAPTACION.aceptar(id), { postular });
  }

  urlDescarga(id: string, formato: 'pdf' | 'docx'): string {
    return API_ADAPTACION.descarga(id, formato);
  }
}
