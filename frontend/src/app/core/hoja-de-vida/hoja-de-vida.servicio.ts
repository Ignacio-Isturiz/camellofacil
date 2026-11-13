import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, switchMap } from 'rxjs';

import { API_HOJA_DE_VIDA } from './hoja-de-vida.api';
import {
  Borrador,
  CambioPropuesto,
  DatosExtraidos,
  DocumentoHoja,
  EstadoCarga,
  Plantilla,
  PlantillaDetalle,
  PropuestaReorganizacion,
  PuntajeAts,
  ResumenHojaDeVida,
  SolicitudBorrador,
  SolicitudReorganizacion,
  ValoresHoja,
  Version,
} from './hoja-de-vida.modelos';

/** Si la primera respuesta tarda más que esto, se avisa «Preparando tu espacio…» (el servidor gratuito despierta lento). */
const ESPERA_ARRANQUE_MS = 4000;

/**
 * Datos del módulo «Hoja de vida». Todo viene del API real con HttpClient; mientras no exista,
 * cada pantalla muestra su estado de error (con «Reintentar») y no se rompe.
 * No se guarda nada de la hoja en localStorage ni sessionStorage (Ley 1581).
 */
@Injectable({ providedIn: 'root' })
export class HojaDeVidaServicio {
  private readonly http = inject(HttpClient);

  readonly resumen = signal<ResumenHojaDeVida | null>(null);
  readonly estadoResumen = signal<EstadoCarga>('cargando');
  /** Verdadero cuando la primera respuesta se está demorando. */
  readonly despertando = signal(false);

  readonly plantillas = signal<readonly Plantilla[]>([]);
  readonly estadoPlantillas = signal<EstadoCarga>('cargando');

  readonly versiones = signal<readonly Version[]>([]);
  readonly estadoVersiones = signal<EstadoCarga>('cargando');

  readonly puntajeAts = signal<PuntajeAts | null>(null);
  readonly estadoPuntajeAts = signal<EstadoCarga>('listo');

  cargarResumen(): void {
    this.estadoResumen.set('cargando');
    const aviso = setTimeout(() => this.despertando.set(true), ESPERA_ARRANQUE_MS);
    this.http.get<ResumenHojaDeVida>(API_HOJA_DE_VIDA.resumen).subscribe({
      next: (r) => {
        clearTimeout(aviso);
        this.despertando.set(false);
        this.resumen.set(r);
        this.puntajeAts.set(r.hoja?.ats ?? null);
        this.estadoResumen.set('listo');
      },
      error: () => {
        clearTimeout(aviso);
        this.despertando.set(false);
        this.estadoResumen.set('error');
      },
    });
  }

  readonly datos = signal<DatosExtraidos | null>(null);
  readonly estadoDatos = signal<EstadoCarga>('cargando');

  cargarDatos(): void {
    this.estadoDatos.set('cargando');
    this.http.get<DatosExtraidos>(API_HOJA_DE_VIDA.datos).subscribe({
      next: (d) => {
        this.datos.set(d);
        this.estadoDatos.set('listo');
      },
      error: () => this.estadoDatos.set('error'),
    });
  }

  /** Guarda lo corregido y confirma: el backend analiza Harvard / ATS con exactamente esa información. */
  guardarYConfirmar(valores: ValoresHoja): Observable<unknown> {
    return this.http
      .put<void>(API_HOJA_DE_VIDA.datos, valores)
      .pipe(switchMap(() => this.http.post<void>(API_HOJA_DE_VIDA.confirmarDatos, {})));
  }

  cargarPlantillas(): void {
    this.estadoPlantillas.set('cargando');
    this.http.get<readonly Plantilla[]>(API_HOJA_DE_VIDA.plantillas).subscribe({
      next: (lista) => {
        this.plantillas.set(lista);
        this.estadoPlantillas.set('listo');
      },
      error: () => this.estadoPlantillas.set('error'),
    });
  }

  cargarVersiones(): void {
    this.estadoVersiones.set('cargando');
    this.http.get<readonly Version[]>(API_HOJA_DE_VIDA.versiones).subscribe({
      next: (lista) => {
        this.versiones.set(lista);
        this.estadoVersiones.set('listo');
      },
      error: () => this.estadoVersiones.set('error'),
    });
  }

  /** Recalcula el puntaje ATS con otra opción de evaluación. */
  evaluarAts(opcionId: string): void {
    this.estadoPuntajeAts.set('cargando');
    this.http.get<PuntajeAts>(API_HOJA_DE_VIDA.puntajeAts(opcionId)).subscribe({
      next: (p) => {
        this.puntajeAts.set(p);
        this.estadoPuntajeAts.set('listo');
      },
      error: () => this.estadoPuntajeAts.set('error'),
    });
  }

  // ---------- Llamadas puntuales (el estado de pantalla lo lleva cada componente) ----------

  plantillaDetalle(id: string): Observable<PlantillaDetalle> {
    return this.http.get<PlantillaDetalle>(API_HOJA_DE_VIDA.plantilla(id));
  }

  vistaPreviaPlantilla(id: string, idioma: string): Observable<DocumentoHoja> {
    return this.http.get<DocumentoHoja>(API_HOJA_DE_VIDA.vistaPreviaPlantilla(id, idioma));
  }

  documentoVersion(id: string): Observable<DocumentoHoja> {
    return this.http.get<DocumentoHoja>(API_HOJA_DE_VIDA.documentoVersion(id));
  }

  renombrarVersion(id: string, nombre: string): Observable<Version> {
    return this.http.patch<Version>(API_HOJA_DE_VIDA.version(id), { nombre });
  }

  duplicarVersion(id: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.duplicarVersion(id), {});
  }

  usarVersionComoBase(id: string): Observable<unknown> {
    return this.http.post(API_HOJA_DE_VIDA.usarVersionComoBase(id), {});
  }

  eliminarVersion(id: string): Observable<unknown> {
    return this.http.delete(API_HOJA_DE_VIDA.version(id));
  }

  proponerReorganizacion(solicitud: SolicitudReorganizacion): Observable<PropuestaReorganizacion> {
    return this.http.post<PropuestaReorganizacion>(API_HOJA_DE_VIDA.reorganizaciones, solicitud);
  }

  /** Aplica o revierte un cambio; el backend devuelve la propuesta recalculada. */
  cambiarCambio(propuestaId: string, cambio: CambioPropuesto, aplicado: boolean): Observable<PropuestaReorganizacion> {
    return this.http.put<PropuestaReorganizacion>(API_HOJA_DE_VIDA.cambioReorganizacion(propuestaId, cambio.id), {
      aplicado,
    });
  }

  aceptarReorganizacion(propuestaId: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.aceptarReorganizacion(propuestaId), {});
  }

  descartarReorganizacion(propuestaId: string): Observable<unknown> {
    return this.http.delete(API_HOJA_DE_VIDA.reorganizacion(propuestaId));
  }

  crearBorrador(solicitud: SolicitudBorrador): Observable<Borrador> {
    return this.http.post<Borrador>(API_HOJA_DE_VIDA.borradores, solicitud);
  }

  guardarBorrador(id: string, valores: ValoresHoja): Observable<Borrador> {
    return this.http.put<Borrador>(API_HOJA_DE_VIDA.borrador(id), valores);
  }

  guardarBorradorComoVersion(id: string): Observable<Version> {
    return this.http.post<Version>(API_HOJA_DE_VIDA.guardarBorradorComoVersion(id), {});
  }
}
